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
 * Agente A* — Práctica 1 TSI (UGR 2025-26)
 *
 * Modelo propio de estado ligero (sin advance()/copy()).
 * Catapultas: itype 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT.
 * Heurística: Manhattan considerando llave y portal.
 * Orden expansión: R, U, L, D (+ NIL para fases catapulta).
 * Desempate: menor f → menor h → orden de inserción (FIFO).
 */
public class AgenteAStar extends AbstractPlayer {

    private int blockSize, gridW, gridH;
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;

    private long[] monPos;
    private int numMon;
    // private int llaveX = -1, llaveY = -1;
    private long[] llavePos;
    private int numLlaves;
    private boolean catapultasGratis;

    private ArrayList<ACTIONS> plan = null;
    private int nodosExp = 0, profMax = 0;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN
    };

    // Direcciones confirmadas: 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT
    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0, 1};   // DOWN
            case 6: return new int[]{0, -1};  // UP
            case 7: return new int[]{1, 0};   // RIGHT
            case 8: return new int[]{-1, 0};  // LEFT
            default: return null;
        }
    }

    // =========================================================
    //  CONSTRUCTOR
    // =========================================================
    public AgenteAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // Portal
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // Clasificar inmovables
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
                        muro[x][y] = true;  // agua = muro para caminar
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

        // Monedas y llave
        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        ArrayList<Long> kl = new ArrayList<>();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) ml.add(enc(gx(obs.position), gy(obs.position)));
                    else if (obs.itype == 16) { kl.add(enc(gx(obs.position), gy(obs.position))); }
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

        System.out.println("Numero de monedas: " + numMon);
        System.out.println("Numero de llaves: " + numLlaves);
    }

    // =========================================================
    //  ACT
    // =========================================================
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) plan = buscarAStar();
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  A* (pseudocódigo diapositiva pág. 26)
    //
    //  abiertos = [inicial]
    //  cerrados = []
    //  while True:
    //      actual = mejorCandidato(abiertos)       // menor f(n)
    //      if actual == objetivo: break
    //      abiertos.remove(actual); cerrados.add(actual)
    //      foreach sucesor in expandir(actual):
    //          if cerrados.contains(sucesor) and mejorCaminoA(sucesor):
    //              cerrados.remove(sucesor); abiertos.add(sucesor)
    //          elif not cerrados.contains(sucesor) and not abiertos.contains(sucesor):
    //              abiertos.add(sucesor)
    //          elif abiertos.contains(sucesor) and mejorCaminoA(sucesor):
    //              abiertos.update(sucesor)
    // =========================================================
    private ArrayList<ACTIONS> buscarAStar() {
        PriorityQueue<Nodo> ab = new PriorityQueue<>();
        HashMap<String, Nodo> abM = new HashMap<>();   // abiertos por key
        HashMap<String, Nodo> ce = new HashMap<>();     // cerrados por key
        int ord = 0;

        // boolean tieneLlaveInicial = (llaveX == -1);
        Estado e0 = new Estado(iniX, iniY, 0, false,
            (1 << numMon) - 1, 
            (1 << numLlaves) - 1,
            (1 << numCats) - 1, 
            0, 0, 0);
        Nodo n0 = new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, heuristica(e0), 0, ord++);
        ab.add(n0);
        abM.put(e0.key(), n0);
        Nodo meta = null;

        while (!ab.isEmpty()) {
            Nodo ac = ab.poll();
            if (ac.obs) continue;  // nodo obsoleto (fue reemplazado)
            String ka = ac.e.key();
            if (abM.get(ka) != ac) continue;  // ya no es el vigente
            abM.remove(ka);

            if (ac.pr > profMax) profMax = ac.pr;

            // ¿Es meta?
            if (esMeta(ac.e)) { meta = ac; break; }

            nodosExp++;
            

            

            ce.put(ka, ac);

            // Expandir sucesores
            ACTIONS[] acciones;
            if (ac.e.fase == 0) {
                acciones = ORDEN;
            } else {
                acciones = new ACTIONS[]{ACTIONS.ACTION_NIL};
            }

            for (ACTIONS a : acciones) {
                Estado h = trans(ac.e, a);
                if (h == null) continue;
                int gN = ac.g + 1;
                double hN = heuristica(h);
                String kS = h.key();

                Nodo eC = ce.get(kS);
                if (eC != null) {
                    // En cerrados: ¿mejor camino?
                    if (gN < eC.g) {
                        ce.remove(kS);
                        Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord++);
                        ab.add(ns);
                        abM.put(kS, ns);
                    }
                    continue;
                }

                Nodo eA = abM.get(kS);
                if (eA == null) {
                    // No en abiertos ni cerrados: añadir
                    Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord++);
                    ab.add(ns);
                    abM.put(kS, ns);
                } else if (gN < eA.g) {
                    // En abiertos con peor g: actualizar
                    eA.obs = true;  // marcar antiguo como obsoleto
                    Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord++);
                    ab.add(ns);
                    abM.put(kS, ns);
                }
            }
        }

        // Reconstruir plan
        ArrayList<ACTIONS> r = new ArrayList<>();
        if (meta != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) p.push(n.accion);
            while (!p.isEmpty()) r.add(p.pop());
        }

        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp);
        mp.setProfundidadMaxima(profMax);
        mp.setNodosAbiertos(abM.size());
        mp.setNodosCerrados(ce.size());
        mp.setNumAccionesPlan(meta != null ? r.size() : -1);
        mp.printMetrics();
        return r;

        
    }

    // =========================================================
    //  HEURÍSTICA: Manhattan considerando llave
    // =========================================================
    private double heuristica(Estado e) {
        return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
    }




    // =========================================================
    //  META Y TRANSICIÓN
    // =========================================================
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

            int m = e.mon; boolean l = e.llave; int mB = e.mB, lB = e.lB, cB = e.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            // if (nx == llaveX && ny == llaveY && !l) l = true;
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }

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
            // if (nx == llaveX && ny == llaveY && !l) l = true;
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }

            if (nx == metaX && ny == metaY && l) {
                return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            }

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

    // =========================================================
    //  UTILIDADES
    // =========================================================
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

    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }


    // =========================================================
    //  CLASES INTERNAS
    // =========================================================
    private static class Estado {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        Estado(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }

        String key() {
            return x + "," + y + "," + mon + "," + (llave ? 1 : 0) + ","
                 + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }

    private static class Nodo implements Comparable<Nodo> {
        Estado e; Nodo padre; ACTIONS accion;
        int g, pr, orden;
        double h;
        boolean obs;  // obsoleto (reemplazado por mejor camino)

        Nodo(Estado e, Nodo p, ACTIONS a, int g, double h, int pr, int o) {
            this.e = e; padre = p; accion = a;
            this.g = g; this.h = h; this.pr = pr; orden = o;
        }

        double f() { return g + h; }

        @Override
        public int compareTo(Nodo o) {
            double f1 = f(), f2 = o.f();
            if (f1 != f2) return Double.compare(f1, f2);
            if (h != o.h) return Double.compare(h, o.h);
            return Integer.compare(orden, o.orden);
        }
    }
}