package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.*;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

public class AgenteProfundidad extends AbstractPlayer {

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

    // Orden DFS: RIGHT, UP, LEFT, DOWN
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
    public AgenteProfundidad(StateObservation so, ElapsedCpuTimer timer) {
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
        numLlaves = kl.size();
        llavePos = new long[numLlaves];
        for (int i = 0; i < numLlaves; i++) llavePos[i] = kl.get(i);
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);
        catapultasGratis = (numMon == 0);

        System.out.println("Monedas:");
        for (int i = 0; i < numMon; i++) {
            int mx = (int)(monPos[i] % gridW);
            int my = (int)(monPos[i] / gridW);
            System.out.println("  idx=" + i + " pos=(" + mx + "," + my + ")");
        }
        System.out.println("Catapultas (idx orden):");
        for (Map.Entry<Long, Integer> e : catIdx.entrySet()) {
            long pk = e.getKey();
            int cx = (int)(pk % gridW), cy = (int)(pk / gridW);
            int[] d = catDir.get(pk);
            System.out.println("  idx=" + e.getValue() + " pos=(" + cx + "," + cy + ") dir=(" + d[0] + "," + d[1] + ")");
        }
    }

    // =========================================================
    //  ACT
    // =========================================================
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) plan = buscarDFS();
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  DFS RECURSIVO (pseudocódigo diapositiva pág.14)
    //
    //  DFS(inicial, objetivo):
    //      estado[inicial] = VISITADO
    //      padre[inicial] = null
    //      DFS_search(inicial, objetivo)
    //
    //  DFS_search(u, objetivo):
    //      if u == objetivo: return TRUE       ← u es nodo expandido
    //      for each v in sucesores(u):
    //          if estado[v] == NOVISITADO:
    //              estado[v] = VISITADO
    //              padre[v] = u
    //              if DFS_search(v, objetivo): return TRUE
    //      return FALSE
    // =========================================================

    // Variables compartidas para la recursión
    private HashSet<String> visitados;
    private HashMap<String, String> padreKey;    // key hijo → key padre
    private HashMap<String, ACTIONS> padreAccion; // key hijo → acción que llevó del padre al hijo
    private String metaKey;

    private ArrayList<ACTIONS> buscarDFS() {
        visitados = new HashSet<>();
        padreKey = new HashMap<>();
        padreAccion = new HashMap<>();
        metaKey = null;

        // boolean tieneLlaveInicial = (llaveX == -1);
        Estado e0 = new Estado(iniX, iniY, 0, false,
                (1 << numMon) - 1, 
                (1 << numLlaves) - 1, 
                (1 << numCats) - 1, 
                0, 0, 0);

        String k0 = e0.key();
        visitados.add(k0);
        padreKey.put(k0, null);

        dfsSearch(e0);

        // Reconstruir plan
        ArrayList<ACTIONS> r = new ArrayList<>();
        if (metaKey != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            String k = metaKey;
            while (padreKey.get(k) != null) {
                p.push(padreAccion.get(k));
                k = padreKey.get(k);
            }
            while (!p.isEmpty()) r.add(p.pop());
        }

        System.out.println("=== PLAN ===");
        for (int i = 0; i < r.size(); i++) {
            System.out.println(r.get(i));
        }
        System.out.println("Total acciones: " + r.size());

        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp);
        mp.setProfundidadMaxima(profMax);
        mp.setNumAccionesPlan(metaKey != null ? r.size() : -1);
        mp.printMetrics();
        return r;
    }

    private boolean dfsSearch(Estado u) {
        String uk = u.key();
        int depth = 0;
        // Calcular profundidad recorriendo padres
        String k = uk;
        while (padreKey.get(k) != null) { depth++; k = padreKey.get(k); }
        if (depth > profMax) profMax = depth;

        // Comprobar meta (u es nodo expandido)
        if (esMeta(u)) {
            metaKey = uk;
            // nodosExp++; // el nodo meta se cuenta como expandido
            return true;
        }
        nodosExp++;

        // Generar sucesores según la fase
        if (u.fase == 0) {
            // Fase normal: expandir en orden RIGHT, UP, LEFT, DOWN
            for (ACTIONS a : ORDEN) {
                Estado v = trans(u, a);
                if (v == null) continue;
                String vk = v.key();
                if (!visitados.contains(vk)) {
                    visitados.add(vk);
                    padreKey.put(vk, uk);
                    padreAccion.put(vk, a);
                    if (dfsSearch(v)) return true;
                }
            }
        } else {
            // Fases catapulta (1,2,3): solo ACTION_NIL
            Estado v = trans(u, ACTIONS.ACTION_NIL);
            if (v != null) {
                String vk = v.key();
                if (!visitados.contains(vk)) {
                    visitados.add(vk);
                    padreKey.put(vk, uk);
                    padreAccion.put(vk, ACTIONS.ACTION_NIL);
                    if (dfsSearch(v)) return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    //  MODELO DE ESTADO Y TRANSICIÓN
    // =========================================================
    private boolean esMeta(Estado e) {
        return e.x == metaX && e.y == metaY && e.llave && e.fase == 0;
    }

    private Estado trans(Estado e, ACTIONS a) {
        if (e.fase == 0) {
            int m = e.mon; 
            boolean l = e.llave; 
            int mB = e.mB, cB = e.cB, lB = e.lB;

            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = e.x + d[0], ny = e.y + d[1];
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            if (muro[nx][ny]) return null;
            //if (nx == metaX && ny == metaY && !e.llave) return null;
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }
            
            

            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            // if (nx == llaveX && ny == llaveY && !l) l = true;

            int llaveIdx = llaveIdx(nx, ny);
            if (llaveIdx >= 0 && (lB & (1 << llaveIdx)) != 0) {
                lB &= ~(1 << llaveIdx); // marcar como recogida
                l = true;               // ahora el avatar tiene llave
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
            int m = e.mon; 
            boolean l = e.llave; 
            int mB = e.mB, cB = e.cB, lB = e.lB;

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
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            //if (nx == llaveX && ny == llaveY && !l) l = true;
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }

            // Si aterrizamos en el portal con llave, detenemos el vuelo
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
                 + mB + "," + lB + "," + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }
}