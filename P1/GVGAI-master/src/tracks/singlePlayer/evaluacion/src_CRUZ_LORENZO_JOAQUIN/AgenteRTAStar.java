package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.*;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

public class AgenteRTAStar extends AbstractPlayer {

    private int blockSize, gridW, gridH;
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;

    private long[] monPos;
    private int numMon;
    private long[] llavePos;
    private int numLlaves;
    private boolean catapultasGratis;

    private Estado actual;
    private HashMap<String, Double> tablaH;
    private int nodosExp = 0;
    private boolean finalizado = false;
    private long t0 = -1;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT, ACTIONS.ACTION_DOWN
    };

    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0,  1};
            case 6: return new int[]{0, -1};
            case 7: return new int[]{1,  0};
            case 8: return new int[]{-1, 0};
            default: return null;
        }
    }

    public AgenteRTAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        muro  = new boolean[gridW][gridH];
        agua  = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();
        tablaH = new HashMap<>();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // Portal
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // Inmovables
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

        // Monedas y llaves
        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Long> kl = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) {
                        ml.add(enc(gx(obs.position), gy(obs.position)));
                    } else if (obs.itype == 16) {
                        int kx = gx(obs.position), ky = gy(obs.position);
                        if (kx != iniX || ky != iniY)
                            kl.add(enc(kx, ky));
                    }
                }
            }
        }
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);
        numLlaves = kl.size();
        llavePos  = new long[numLlaves];
        for (int i = 0; i < numLlaves; i++) llavePos[i] = kl.get(i);
        catapultasGratis = (numMon == 0);

        actual = new Estado(iniX, iniY, 0, false,
                (1 << numMon)    - 1,
                (1 << numLlaves) - 1,
                (1 << numCats)   - 1,
                0, 0, 0);
    }

    // =========================================================
    // ACT — implementación fiel al pseudocódigo RTA*
    //
    // actual = nodo_inicial
    // while True:
    //   if actual == objetivo: break
    //   S = sucesores(actual)
    //   foreach sucesor in S:
    //     f(sucesor) = h(sucesor) + distance(actual, sucesor)
    //   z          = argmin f(y) para y en S
    //   segundo_min = Segundo_Mínimo({f(y) | y en S})
    //   h(actual)  = max(h(actual), segundo_min)
    //   actual     = z
    // =========================================================
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (finalizado) return ACTIONS.ACTION_NIL;
        if (t0 < 0) t0 = System.currentTimeMillis();

        nodosExp++;

        // if actual == objetivo: break
        if (esMeta(actual)) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // S = sucesores(actual)
        ACTIONS[] acciones = (actual.fase == 0) ? ORDEN
                : new ACTIONS[]{ACTIONS.ACTION_NIL};

        List<ACTIONS> accionesValidas  = new ArrayList<>();
        List<Estado>  sucesoresValidos = new ArrayList<>();
        List<Double>  costesF         = new ArrayList<>();

        for (ACTIONS a : acciones) {
            Estado suc = trans(actual, a);
            if (suc == null) continue;
            // f(sucesor) = h(sucesor) + distance(actual, sucesor)  [distance=1]
            double fSuc = getH(suc) + 1.0;
            accionesValidas.add(a);
            sucesoresValidos.add(suc);
            costesF.add(fSuc);
        }

        if (accionesValidas.isEmpty()) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // z = argmin f(y)  [desempate: orden R,U,L,D]
        int mejorIdx = 0;
        double mejorF = costesF.get(0);
        for (int i = 1; i < costesF.size(); i++) {
            if (costesF.get(i) < mejorF) {
                mejorF   = costesF.get(i);
                mejorIdx = i;
            }
        }

        // segundo_min = Segundo_Mínimo({f(y) | y en S})
        double segundoMin;
        if (costesF.size() == 1) {
            segundoMin = costesF.get(0);
        } else {
            // Ordenamos para sacar el segundo valor más pequeño
            List<Double> sorted = new ArrayList<>(costesF);
            Collections.sort(sorted);
            segundoMin = sorted.get(1);
        }

        // h(actual) = max(h(actual), segundo_min)
        String keyActual = keyEstado(actual);
        Double hActual = tablaH.get(keyActual);
        if (hActual == null) hActual = heuristicaBase(actual);
        tablaH.put(keyActual, Math.max(hActual, segundoMin));

        // actual = z
        actual = sucesoresValidos.get(mejorIdx);

        if (esMeta(actual)) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
        }

        return accionesValidas.get(mejorIdx);
    }

    // =========================================================
    // TABLA HEURÍSTICA — clave estado completo
    // =========================================================
    private double getH(Estado e) {
        String k = keyEstado(e);
        Double v = tablaH.get(k);
        if (v != null) return v;
        double h = heuristicaBase(e);
        tablaH.put(k, h);
        return h;
    }

    private double heuristicaBase(Estado e) {
        return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
    }

    private boolean esMeta(Estado e) {
        return e.x == metaX && e.y == metaY && e.llave && e.fase == 0;
    }

    // =========================================================
    // TRANSICIÓN (igual que AgenteAStar con bitmask de llaves)
    // =========================================================
    private Estado trans(Estado e, ACTIONS a) {
        if (e.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = e.x + d[0], ny = e.y + d[1];
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            if (muro[nx][ny]) return null;
            if (nx == metaX && ny == metaY && !e.llave) return null;

            int m = e.mon; boolean l = e.llave; int mB = e.mB, lB = e.lB, cB = e.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);
                if (!l) l = true;
            }
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                if (!catapultasGratis && m <= 0) return null;
                if (!catapultasGratis) m--;
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Estado(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }
            return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        } else if (e.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB, 2, e.vdx, e.vdy);

        } else if (e.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = e.x + e.vdx, ty = e.y + e.vdy;
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);
            if (!col && tx == metaX && ty == metaY && !e.llave) col = true;
            if (col) {
                if (agua[e.x][e.y]) return null;
                return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB, 0, 0, 0);
            }
            int nx = tx, ny = ty;
            int m = e.mon; boolean l = e.llave; int mB = e.mB, lB = e.lB, cB = e.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);
                if (!l) l = true;
            }
            if (nx == metaX && ny == metaY && l)
                return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Estado(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }
            return new Estado(nx, ny, m, l, mB, lB, cB, 2, e.vdx, e.vdy);

        } else if (e.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB, 2, e.vdx, e.vdy);
        }
        return null;
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

    private String keyEstado(Estado e) {
        return e.x + "," + e.y + "," + e.mon + "," + (e.llave ? 1 : 0) + ","
                + e.mB + "," + e.lB + "," + e.cB + "," + e.fase + "," + e.vdx + "," + e.vdy;
    }

    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }

    // =========================================================
    // ESTADO
    // =========================================================
    private static class Estado {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        Estado(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }
    }
}