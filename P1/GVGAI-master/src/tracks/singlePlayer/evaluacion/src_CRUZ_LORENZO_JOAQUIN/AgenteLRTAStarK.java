package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

/**
 * Agente LRTA*(k) (Learning Real-Time A*) — Práctica 1 TSI
 * Implementa propagación de heurística con parámetro K.
 */
public class AgenteLRTAStarK extends AbstractPlayer {

    // Parámetro K de propagación
    private static final int K = 5; 

    private int blockSize, gridW, gridH;
    private int metaX, metaY;
    private boolean[][] muro, agua;

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;
    private long[] monPos;
    private int numMon;
    private long[] llavePos;
    private int numLlaves;
    private boolean catapultasGratis;
    private int iniX, iniY;

    // --- Estructuras LRTA*(k) ---
    private HashMap<String, Double> tablaH;
    private HashMap<String, String> soporte; // Guarda el key del mejor vecino

    // Métricas oficiales
    private int nodosExp = 0, numAcciones = 0, numActualizacionesTabla = 0;
    private long tiempoInicio;
    private boolean haTerminado = false;

    private Estado actual;


    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT,
        ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,
        ACTIONS.ACTION_DOWN
    };

    public AgenteLRTAStarK(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();
        
        tablaH = new HashMap<>();
        soporte = new HashMap<>();

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
                        agua[x][y] = true; muro[x][y] = true;
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

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Long> kl = new ArrayList<>();
        ArrayList[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) ml.add(enc(gx(obs.position), gy(obs.position)));
                    else if (obs.itype == 16) {
                        int kx = gx(obs.position), ky = gy(obs.position);
                        if (kx != iniX || ky != iniY) kl.add(enc(kx, ky));
                    }
                }
            }
        }
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);
        numLlaves = kl.size();
        llavePos = new long[numLlaves];
        for (int i = 0; i < numLlaves; i++) llavePos[i] = kl.get(i);
        catapultasGratis = (numMon == 0);

        tiempoInicio = System.currentTimeMillis();
    }

    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (haTerminado) return ACTIONS.ACTION_NIL;

        nodosExp++;

        // Inicializar actual en el primer tick — DEBE ir ANTES de cualquier uso
        if (actual == null) {
            actual = new Estado(iniX, iniY, 0, false,
                (1 << numMon) - 1,
                (1 << numLlaves) - 1,
                (1 << numCats) - 1,
                0, 0, 0);
        }

        // Comprobar meta antes de actuar
        if (esMeta(actual)) {
            finalizarBusqueda();
            return ACTIONS.ACTION_NIL;
        }

        // 1. PRIMERO: Actualizar H con lookahead
        lookaheadUpdateK(actual, K);

        // 2. DESPUÉS: Elegir mejor sucesor con H ya actualizada
        double mejorF = Double.MAX_VALUE;
        ACTIONS mejorAccion = ACTIONS.ACTION_NIL;
        Estado mejorSucesor = null;

        ACTIONS[] accionesDisp = (actual.fase == 0) ? ORDEN
            : new ACTIONS[]{ACTIONS.ACTION_NIL};

        for (ACTIONS a : accionesDisp) {
            Estado suc = trans(actual, a);
            if (suc == null) continue;
            double f = 1.0 + obtenerH(suc);
            if (f < mejorF) {
                mejorF = f;
                mejorAccion = a;
                mejorSucesor = suc;
            }
        }

        if (mejorSucesor == null) {
            finalizarBusqueda();
            return ACTIONS.ACTION_NIL;
        }

        // 3. Moverse
        actual = mejorSucesor;
        numAcciones++;

        if (esMeta(actual)) {
            finalizarBusqueda();
        }

        return mejorAccion;
    }

    // =========================================================
    //  PROPAGACIÓN LRTA*(k)
    // =========================================================
    private void lookaheadUpdateK(Estado inicio, int limiteK) {
        Queue<Estado> cola = new LinkedList<>();
        cola.add(inicio);
        int contador = limiteK - 1;  // contador = k-1

        while (!cola.isEmpty()) {
            Estado x = cola.poll();

            // Calcular mejor vecino (soporte)
            double minF = Double.MAX_VALUE;
            Estado mejorVecino = null;

            ACTIONS[] acciones = (x.fase == 0) ? ORDEN
                : new ACTIONS[]{ACTIONS.ACTION_NIL};

            for (ACTIONS a : acciones) {
                Estado vecino = trans(x, a);
                if (vecino == null) continue;
                double f = 1.0 + obtenerH(vecino);
                if (f < minF) {
                    minF = f;
                    mejorVecino = vecino;
                }
            }

            if (mejorVecino == null) continue;

            // soporte(x) = mejorVecino
            soporte.put(x.key(), mejorVecino.key());

            // Regla de aprendizaje LRTA*
            boolean propagar = false;
            double hActual = obtenerH(x);
            if (hActual < minF) {
                propagar = true;
                tablaH.put(x.key(), minF);
                numActualizacionesTabla++;
            }

            // Propagar a sucesores cuyo soporte es x
            if (propagar) {
                for (ACTIONS a : acciones) {
                    Estado sucesor = trans(x, a);
                    if (sucesor == null) continue;
                    if (contador > 0 && x.key().equals(soporte.get(sucesor.key()))) {
                        cola.add(sucesor);
                        contador--;
                    }
                }
            }
        }
    }

    // =========================================================
    //  RESTO DE FUNCIONES (Idénticas a RTA*)
    // =========================================================
    private double obtenerH(Estado e) {
        String k = e.key();
        if (tablaH.containsKey(k)) return (double) tablaH.get(k);
        double h0 = heuristica(e);
        tablaH.put(k, h0);
        return h0;
    }

    private double heuristica(Estado e) {
        return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
    }

    private boolean esMeta(Estado e) {
        return e.x == metaX && e.y == metaY && e.llave && e.fase == 0;
    }

    private void finalizarBusqueda() {
        haTerminado = true;
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp);
        mp.setNumAccionesPlan(numAcciones);
        mp.setNumActualizacionesTabla(numActualizacionesTabla); // Nueva métrica para LRTA*
        mp.setTiempoMilisegundos(tiempoTotal);
        mp.setAgente("LRTA*(k)");
        mp.printMetrics();
    }

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
            if (li >= 0 && (lB & (1 << li)) != 0) { lB &= ~(1 << li); if (!l) l = true; }
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
            if (li >= 0 && (lB & (1 << li)) != 0) { lB &= ~(1 << li); if (!l) l = true; }
            if (nx == metaX && ny == metaY && l)
                return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk); cB &= ~(1 << ci);
                return new Estado(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }
            return new Estado(nx, ny, m, l, mB, lB, cB, 2, e.vdx, e.vdy);

        } else if (e.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB, 2, e.vdx, e.vdy);
        }
        return null;
    }

    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }

    private int gx(Vector2d p) { return (int)(p.x / blockSize); }
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }
    private long enc(int x, int y) { return (long)y * gridW + x; }
    private int[] delta(ACTIONS a) {
        switch(a){
            case ACTION_RIGHT: return new int[]{1, 0};
            case ACTION_LEFT:  return new int[]{-1, 0};
            case ACTION_UP:    return new int[]{0, -1};
            case ACTION_DOWN:  return new int[]{0, 1};
            default: return new int[]{0, 0};
        }
    }
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for(int i = 0; i < numMon; i++) if(monPos[i] == k) return i;
        return -1;
    }

    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0, 1};
            case 6: return new int[]{0, -1};
            case 7: return new int[]{1, 0};
            case 8: return new int[]{-1, 0};
            default: return null;
        }
    }

    private static class Estado {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy; boolean llave;
        Estado(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x=x; this.y=y; mon=m; llave=l;
            this.mB=mB; this.lB=lB; this.cB=cB; fase=f; vdx=vx; vdy=vy;
        }
        String key() { 
            return x+","+y+","+(llave?1:0)+","+mB+","+lB+","+cB+","+mon+","+fase+","+vdx+","+vdy; 
        }
    }

    // =========================================================
    //  FIN DE PARTIDA (GVGAI llama a este método al terminar)
    // =========================================================
    @Override
    public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        if (!haTerminado) {
            finalizarBusqueda();
        }
    }
}