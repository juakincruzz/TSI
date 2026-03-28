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
    private int tipoAvatarNormal;

    private HashMap<Long, int[]> catDir;   // pos -> {dx, dy}
    private HashMap<Long, Integer> catIdx;  // pos -> índice bitmask
    private int numCats;
    private long[] monPos;
    private int numMon;
    private int llaveX = -1, llaveY = -1;
    private boolean catapultasGratis;

    private ArrayList<ACTIONS> plan = null;
    private int nodosExp = 0, profMax = 0;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN
    };

    public AgenteProfundidad(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        tipoAvatarNormal = so.getAvatarType();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // Portal (meta)
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // Recursos: monedas (itype=15) y llave (itype=16)
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

        // === Paso 1: Clasificar inmovables ===
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();

        // Catapultas: itypes que no son muro(0), floor(2), agua(3), portal(18)
        HashSet<Integer> catItypes = new HashSet<>();
        HashMap<Integer, ArrayList<int[]>> catPosByItype = new HashMap<>();

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
                    } else if (obs.itype != 2 && obs.itype != 18) {
                        catItypes.add(obs.itype);
                        catPosByItype.computeIfAbsent(obs.itype, k -> new ArrayList<>())
                                     .add(new int[]{x, y});
                    }
                }
            }
        }

        // === Paso 2: Detectar dirección de cada itype de catapulta ===
        HashMap<Integer, int[]> itypeToDir = detectarDirecciones(so, catItypes, catPosByItype);

        // === Paso 3: Registrar catapultas detectadas ===
        int ci = 0;
        for (Map.Entry<Integer, ArrayList<int[]>> entry : catPosByItype.entrySet()) {
            int[] dir = itypeToDir.get(entry.getKey());
            if (dir == null) continue;
            for (int[] pos : entry.getValue()) {
                long pk = enc(pos[0], pos[1]);
                catDir.put(pk, dir);
                catIdx.put(pk, ci++);
            }
        }
        numCats = ci;

        // Debug
        System.out.println("INI=(" + iniX + "," + iniY + ") META=(" + metaX + "," + metaY + ")");
        System.out.println("Llave=(" + llaveX + "," + llaveY + ") numMon=" + numMon + " numCats=" + numCats);
        System.out.println("catapultasGratis=" + catapultasGratis);
        System.out.println("catItypes=" + catItypes + " detected=" + itypeToDir.keySet());
        for (Map.Entry<Integer, int[]> e : itypeToDir.entrySet()) {
            System.out.println("  itype " + e.getKey() + " -> dir=(" + e.getValue()[0] + "," + e.getValue()[1] + ")");
        }
        System.out.println("--- Mapa ---");
        for (int y = 0; y < gridH; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < gridW; x++) {
                if (x == iniX && y == iniY) sb.append('A');
                else if (x == metaX && y == metaY) sb.append('G');
                else if (x == llaveX && y == llaveY) sb.append('K');
                else if (muro[x][y]) sb.append('#');
                else if (catDir.containsKey(enc(x, y))) sb.append('C');
                else if (agua[x][y]) sb.append('~');
                else if (monIdx(x, y) >= 0) sb.append('$');
                else sb.append('.');
            }
            System.out.println(sb.toString());
        }
    }

    /**
     * BFS estatal: explora estados de juego reales (incluyendo cadenas de catapultas)
     * para detectar la dirección de lanzamiento de cada itype de catapulta.
     * Detecta por:
     *   (a) Adyacencia en forma normal con >=1 moneda → entrar y observar vuelo
     *   (b) Cambio de tipo de avatar al volar sobre una catapulta → observar nueva dirección
     */
    private HashMap<Integer, int[]> detectarDirecciones(
            StateObservation so,
            HashSet<Integer> catItypes,
            HashMap<Integer, ArrayList<int[]>> catPosByItype) {

        // Mapa inverso: posición → itype
        HashMap<Long, Integer> catItypeByPos = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<int[]>> e : catPosByItype.entrySet()) {
            for (int[] pos : e.getValue())
                catItypeByPos.put(enc(pos[0], pos[1]), e.getKey());
        }

        HashMap<Integer, int[]> result = new HashMap<>();
        if (catItypes.isEmpty()) return result;

        ACTIONS[] dirs = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
                          ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN};
        int[][] deltas = {{1,0},{0,-1},{-1,0},{0,1}};

        Queue<StateObservation> q = new LinkedList<>();
        HashSet<String> vis = new HashSet<>();
        q.add(so.copy());
        vis.add(statKey(so));

        int maxIter = 5000;
        while (!q.isEmpty() && maxIter-- > 0 && result.size() < catItypes.size()) {
            StateObservation s = q.poll();
            if (s.isGameOver()) continue;

            boolean isNormal = (s.getAvatarType() == tipoAvatarNormal);
            int ax = gx(s.getAvatarPosition()), ay = gy(s.getAvatarPosition());

            if (isNormal) {
                // (a) Comprobar adyacencia a catapultas no detectadas con >=1 moneda
                int coins = s.getAvatarResources().getOrDefault(15, 0);
                if (coins > 0) {
                    for (int d = 0; d < 4; d++) {
                        int nx = ax + deltas[d][0], ny = ay + deltas[d][1];
                        if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) continue;
                        Integer itAtPos = catItypeByPos.get(enc(nx, ny));
                        if (itAtPos == null || result.containsKey(itAtPos)) continue;

                        StateObservation test = s.copy();
                        test.advance(dirs[d]);
                        if (test.isGameOver()) continue;
                        int sx = gx(test.getAvatarPosition()), sy = gy(test.getAvatarPosition());
                        for (int t = 0; t < 20; t++) {
                            test.advance(ACTIONS.ACTION_NIL);
                            if (test.isGameOver()) break;
                            if (test.getAvatarType() != tipoAvatarNormal) {
                                int bx = gx(test.getAvatarPosition()), by = gy(test.getAvatarPosition());
                                if (bx != sx || by != sy) {
                                    result.put(itAtPos, new int[]{Integer.signum(bx-sx), Integer.signum(by-sy)});
                                    break;
                                }
                            }
                        }
                    }
                }
                // Expandir en forma normal
                for (ACTIONS dir : dirs) {
                    StateObservation child = s.copy();
                    child.advance(dir);
                    if (child.isGameOver()) continue;
                    String k = statKey(child);
                    if (!vis.contains(k)) { vis.add(k); q.add(child); }
                }
            } else {
                // (b) Forma bala: avanzar con NIL y detectar cambio de dirección
                StateObservation child = s.copy();
                child.advance(ACTIONS.ACTION_NIL);
                if (!child.isGameOver()) {
                    if (child.getAvatarType() != s.getAvatarType()) {
                        // El tipo cambió: la bala pasó por una catapulta
                        int bx = gx(child.getAvatarPosition()), by = gy(child.getAvatarPosition());
                        Integer itAtPos = catItypeByPos.get(enc(bx, by));
                        if (itAtPos != null && !result.containsKey(itAtPos)) {
                            StateObservation test = child.copy();
                            test.advance(ACTIONS.ACTION_NIL);
                            if (!test.isGameOver()) {
                                int tx = gx(test.getAvatarPosition()), ty = gy(test.getAvatarPosition());
                                if (tx != bx || ty != by)
                                    result.put(itAtPos, new int[]{Integer.signum(tx-bx), Integer.signum(ty-by)});
                            }
                        }
                    }
                    String k = statKey(child);
                    if (!vis.contains(k)) { vis.add(k); q.add(child); }
                }
            }
        }
        return result;
    }

    private String statKey(StateObservation so) {
        int ax = gx(so.getAvatarPosition()), ay = gy(so.getAvatarPosition());
        int coins = so.getAvatarResources().getOrDefault(15, 0);
        return ax + "," + ay + "," + coins + "," + so.getAvatarType();
    }

    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) plan = buscarDFS();
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    private ArrayList<ACTIONS> buscarDFS() {
        Stack<Nodo> frontera = new Stack<>();
        HashSet<String> visitados = new HashSet<>();

        boolean tieneLlaveInicial = (llaveX == -1);
        Estado e0 = new Estado(iniX, iniY, 0, tieneLlaveInicial,
                (1 << numMon) - 1, (1 << numCats) - 1, 0, 0, 0);
        frontera.push(new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, 0));
        Nodo meta = null;

        while (!frontera.isEmpty()) {
            Nodo ac = frontera.pop();
            String ka = ac.e.key();

            if (visitados.contains(ka)) continue;
            visitados.add(ka);

            if (ac.pr > profMax) profMax = ac.pr;
            if (esMeta(ac.e)) { meta = ac; break; }
            nodosExp++;

            if (ac.e.fase == 0) {
                for (int i = ORDEN.length - 1; i >= 0; i--) {
                    Estado h = trans(ac.e, ORDEN[i]);
                    if (h == null) continue;
                    frontera.push(new Nodo(h, ac, ORDEN[i], ac.g + 1, ac.pr + 1));
                }
            } else {
                Estado h = trans(ac.e, ACTIONS.ACTION_NIL);
                if (h != null) {
                    frontera.push(new Nodo(h, ac, ACTIONS.ACTION_NIL, ac.g + 1, ac.pr + 1));
                }
            }
        }

        ArrayList<ACTIONS> r = new ArrayList<>();
        if (meta != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) p.push(n.accion);
            while (!p.isEmpty()) r.add(p.pop());
        }

        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp);
        mp.setProfundidadMaxima(profMax);
        mp.setNumAccionesPlan(meta != null ? r.size() : -1);
        mp.printMetrics();
        return r;
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
            if (agua[nx][ny]) return null;
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
            if (!col) col = muro[tx][ty] || (tx == metaX && ty == metaY && !e.llave);

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
        String key() {
            return x + "," + y + "," + mon + "," + (llave ? 1 : 0) + ","
                 + mB + "," + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }

    private static class Nodo {
        Estado e; Nodo padre; ACTIONS accion; int g, pr;
        Nodo(Estado e, Nodo padre, ACTIONS accion, int g, int pr) {
            this.e = e; this.padre = padre; this.accion = accion;
            this.g = g; this.pr = pr;
        }
    }
}