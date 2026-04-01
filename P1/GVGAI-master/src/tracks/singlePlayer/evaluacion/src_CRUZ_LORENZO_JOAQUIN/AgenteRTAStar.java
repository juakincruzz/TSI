package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.*;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

/**
 * Agente RTA* — Práctica 1 TSI (UGR 2025-26)
 *
 * Búsqueda en tiempo real: en cada tick expande el nodo actual,
 * se mueve al mejor vecino y actualiza h(actual) con el 2º mínimo.
 *
 * Heurística: Manhattan considerando llave y portal.
 * Tabla heurística indexada SOLO por posición (x, y).
 * Orden de expansión/desempate: R, U, L, D.
 */
public class AgenteRTAStar extends AbstractPlayer {

    private static final boolean DEBUG = true;

    private int blockSize, gridW, gridH;
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;

    private long[] monPos;
    private int numMon;
    private int llaveX = -1, llaveY = -1;
    private boolean catapultasGratis;

    private Estado actual;
    private HashMap<Long, Double> tablaH;  // clave: enc(x,y) solo posición
    private int nodosExp = 0;
    private boolean finalizado = false;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN
    };

    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0, 1};
            case 6: return new int[]{0, -1};
            case 7: return new int[]{1, 0};
            case 8: return new int[]{-1, 0};
            default: return null;
        }
    }

    public AgenteRTAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();
        tablaH = new HashMap<>();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        ArrayList<long[]> catList = new ArrayList<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;
                    if (obs.itype == 0) {
                        muro[x][y] = true;
                    } else if (obs.itype == 3) {
                        agua[x][y] = true;
                        muro[x][y] = true;
                    } else {
                        int[] dir = catapultDir(obs.itype);
                        if (dir != null) {
                            long pk = enc(x, y);
                            catDir.put(pk, dir);
                            catList.add(new long[]{pk});
                        }
                    }
                }
            }
        }
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) ml.add(enc(gx(obs.position), gy(obs.position)));
                    else if (obs.itype == 16) { llaveX = gx(obs.position); llaveY = gy(obs.position); }
                }
            }
        }
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);
        catapultasGratis = (numMon == 0);

        boolean tieneLlaveInicial = (llaveX == -1);
        actual = new Estado(iniX, iniY, 0, tieneLlaveInicial,
            (1 << numMon) - 1, (1 << numCats) - 1, 0, 0, 0);

        if (DEBUG) {
            System.out.println("=== RTA* DEBUG v5 ===");
            System.out.println("Inicio: (" + iniX + "," + iniY + ") Meta: (" + metaX + "," + metaY + ")");
            System.out.println("Llave: (" + llaveX + "," + llaveY + ") Monedas: " + numMon + " Catapultas: " + numCats);
        }
    }

    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (finalizado) return ACTIONS.ACTION_NIL;

        nodosExp++;

        if (esMeta(actual)) {
            if (DEBUG) System.out.println("META alcanzada en tick " + nodosExp);
            finalizado = true;
            fijarMetricas(true);
            return ACTIONS.ACTION_NIL;
        }

        ACTIONS[] acciones;
        if (actual.fase == 0) {
            acciones = ORDEN;
        } else {
            acciones = new ACTIONS[]{ACTIONS.ACTION_NIL};
        }

        ArrayList<ACTIONS> accionesValidas = new ArrayList<>();
        ArrayList<Estado> sucesoresValidos = new ArrayList<>();
        ArrayList<Double> costesF = new ArrayList<>();

        for (ACTIONS a : acciones) {
            Estado suc = trans(actual, a);
            if (suc == null) continue;
            double hSuc = getH(suc);
            double fSuc = 1.0 + hSuc;
            accionesValidas.add(a);
            sucesoresValidos.add(suc);
            costesF.add(fSuc);
        }

        if (accionesValidas.isEmpty()) {
            if (DEBUG) System.out.println("MUERTE (sin sucesores) tick=" + nodosExp
                + " pos=(" + actual.x + "," + actual.y + ") fase=" + actual.fase
                + " llave=" + actual.llave + " mon=" + actual.mon);
            finalizado = true;
            fijarMetricas(false);
            return ACTIONS.ACTION_NIL;
        }

        int mejorIdx = 0;
        double mejorF = costesF.get(0);
        for (int i = 1; i < costesF.size(); i++) {
            if (costesF.get(i) < mejorF) {
                mejorF = costesF.get(i);
                mejorIdx = i;
            }
        }

        double segundoMin;
        if (costesF.size() == 1) {
            segundoMin = costesF.get(0);
        } else {
            segundoMin = Double.MAX_VALUE;
            for (int i = 0; i < costesF.size(); i++) {
                if (i != mejorIdx && costesF.get(i) < segundoMin) {
                    segundoMin = costesF.get(i);
                }
            }
        }

        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("T").append(nodosExp).append(" (").append(actual.x).append(",").append(actual.y)
              .append(") f=").append(actual.fase)
              .append(" llave=").append(actual.llave ? "Y" : "N")
              .append(" mon=").append(actual.mon)
              .append(" h=").append(String.format("%.0f", getH(actual)));
            sb.append(" | sucs: ");
            for (int i = 0; i < accionesValidas.size(); i++) {
                Estado s = sucesoresValidos.get(i);
                sb.append(accionesValidas.get(i).toString().replace("ACTION_", ""))
                  .append("→(").append(s.x).append(",").append(s.y).append(")")
                  .append(" f=").append(String.format("%.0f", costesF.get(i)));
                if (i == mejorIdx) sb.append("*");
                sb.append("  ");
            }
            sb.append("| 2min=").append(String.format("%.0f", segundoMin));
            System.out.println(sb.toString());
        }

        // Regla de aprendizaje
        long keyActual = enc(actual.x, actual.y);
        double hActual = getH(actual);
        if (segundoMin > hActual) {
            tablaH.put(keyActual, segundoMin);
            if (DEBUG && actual.fase == 0) {
                System.out.println("   UPDATE h(" + actual.x + "," + actual.y + ") "
                    + String.format("%.0f", hActual) + " → " + String.format("%.0f", segundoMin));
            }
        }

        ACTIONS accionElegida = accionesValidas.get(mejorIdx);
        Estado siguiente = sucesoresValidos.get(mejorIdx);

        if (esMeta(siguiente)) {
            nodosExp++;
            if (DEBUG) System.out.println("META en siguiente tick " + nodosExp);
            finalizado = true;
            fijarMetricas(true);
        }

        actual = siguiente;
        return accionElegida;
    }

    // =========================================================
    //  TABLA HEURÍSTICA — clave solo posición (x,y)
    // =========================================================
    private double getH(Estado e) {
        long k = enc(e.x, e.y);
        Double v = tablaH.get(k);
        if (v != null) return v;
        double h = heuristicaBase(e);
        tablaH.put(k, h);
        return h;
    }

    /**
     * Heurística base: Manhattan considerando llave y portal.
     */
    private double heuristicaBase(Estado e) {
        if (e.fase != 0) {
            return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
        }
        if (!e.llave && llaveX >= 0) {
            return Math.abs(e.x - llaveX) + Math.abs(e.y - llaveY)
                 + Math.abs(llaveX - metaX) + Math.abs(llaveY - metaY);
        }
        return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
    }

    private boolean esMeta(Estado e) {
        return e.x == metaX && e.y == metaY && e.llave && e.fase == 0;
    }

    private Estado trans(Estado e, ACTIONS a) {
        if (e.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = e.x + d[0], ny = e.y + d[1];
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            if (muro[nx][ny]) return null;
            if (nx == metaX && ny == metaY && !e.llave) return null;

            int m = e.mon; boolean l = e.llave; int mB = e.mB, cB = e.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            if (nx == llaveX && ny == llaveY && !l) l = true;

            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                if (!catapultasGratis && m <= 0) return null;
                if (!catapultasGratis) m--;
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Estado(nx, ny, m, l, mB, cB, 1, dir[0], dir[1]);
            }
            return new Estado(nx, ny, m, l, mB, cB, 0, 0, 0);

        } else if (e.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.cB, 2, e.vdx, e.vdy);

        } else if (e.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = e.x + e.vdx, ty = e.y + e.vdy;
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);
            if (!col && tx == metaX && ty == metaY && !e.llave) col = true;

            if (col) {
                if (agua[e.x][e.y]) return null;
                return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.cB, 0, 0, 0);
            }

            int nx = tx, ny = ty;
            int m = e.mon; boolean l = e.llave; int mB = e.mB, cB = e.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            if (nx == llaveX && ny == llaveY && !l) l = true;

            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Estado(nx, ny, m, l, mB, cB, 3, dir[0], dir[1]);
            }
            return new Estado(nx, ny, m, l, mB, cB, 2, e.vdx, e.vdy);

        } else if (e.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.cB, 2, e.vdx, e.vdy);
        }
        return null;
    }

    private void fijarMetricas(boolean victoria) {
        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNumAccionesPlan(victoria ? nodosExp : -1);
        mp.setNodosExpandidos(nodosExp);
        mp.printMetrics();
    }

    private int gx(Vector2d p) { return (int)(p.x / blockSize); }
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }
    private long enc(int x, int y) { return (long)y * gridW + x; }

    private int[] delta(ACTIONS a) {
        switch (a) {
            case ACTION_RIGHT: return new int[]{1, 0};
            case ACTION_LEFT:  return new int[]{-1, 0};
            case ACTION_UP:    return new int[]{0, -1};
            case ACTION_DOWN:  return new int[]{0, 1};
            default:           return new int[]{0, 0};
        }
    }

    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numMon; i++) if (monPos[i] == k) return i;
        return -1;
    }

    private static class Estado {
        int x, y, mon, mB, cB, fase, vdx, vdy;
        boolean llave;

        Estado(int x, int y, int m, boolean l, int mB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }
    }
}