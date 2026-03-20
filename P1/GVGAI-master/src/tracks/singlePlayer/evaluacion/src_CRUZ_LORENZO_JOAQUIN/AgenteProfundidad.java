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
 * Agente DFS — Práctica 1 TSI (UGR 2025-26)
 *
 * Búsqueda no informada offline con DFS iterativo.
 * Modelo propio de estado (mismo que AgenteAStar).
 * Búsqueda completa en el primer act().
 *
 * Pseudocódigo DFS:
 *   frontera = Stack con nodo inicial
 *   visitados = {}
 *   while frontera no vacía:
 *     actual = frontera.pop()
 *     if actual == meta: reconstruir plan
 *     if actual ya visitado: saltar
 *     visitados.add(actual)
 *     for each acción in {NIL, DOWN, LEFT, UP, RIGHT}: // orden inverso para Stack
 *       hijo = transición(actual, acción)
 *       frontera.push(hijo)
 *
 * Fases del avatar: 0=tierra, 1=transformación, 2=vuelo, 3=sobrepaso catapulta
 * Orden expansión: R, U, L, D, NIL (se pushean en orden inverso en la Stack).
 */
public class AgenteProfundidad extends AbstractPlayer {

    private int blockSize, gridW, gridH;
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;
    private long[] monPos;
    private int numMon;
    private int llaveX = -1, llaveY = -1;

    private ArrayList<ACTIONS> plan = null;
    private int nodosExp = 0, profMax = 0;

    // Orden de expansión: R, U, L, D, NIL
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN,
        ACTIONS.ACTION_NIL
    };

    // =========================================================
    //  CONSTRUCTOR — idéntico a AgenteAStar
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

        // Inmovables
        ArrayList<int[]> catPositions = new ArrayList<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null)
            for (ArrayList<Observation> lista : inmov)
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;
                    if (obs.itype == 0) muro[x][y] = true;
                    else if (obs.itype == 3) { agua[x][y] = true; muro[x][y] = true; }
                    else if (obs.itype == 5) catPositions.add(new int[]{x, y});
                    // itype=7 se detectará como catapulta abajo
                }

        // Detectar itypes de catapultas
        HashMap<Integer, int[]> itypeToDir = detectarItypeDirecciones(so);

        // Reclasificar inmovables con itypeToDir
        catDir.clear(); catIdx.clear();
        ArrayList<long[]> catList = new ArrayList<>();
        if (inmov != null)
            for (ArrayList<Observation> lista : inmov)
                for (Observation obs : lista) {
                    if (itypeToDir.containsKey(obs.itype)) {
                        int x = gx(obs.position), y = gy(obs.position);
                        long pk = enc(x, y);
                        catDir.put(pk, itypeToDir.get(obs.itype));
                        catList.add(new long[]{pk});
                    }
                }
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        // Monedas y llave
        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null)
            for (ArrayList<Observation> lista : rec)
                for (Observation obs : lista) {
                    if (obs.itype == 15) ml.add(enc(gx(obs.position), gy(obs.position)));
                    else if (obs.itype == 16) { llaveX = gx(obs.position); llaveY = gy(obs.position); }
                }
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);

        System.out.println("DFS init: " + gridW + "x" + gridH
            + " meta=(" + metaX + "," + metaY + ") cats=" + numCats);
    }

    // =========================================================
    //  Detección de itypes de catapultas (idéntica a AgenteAStar)
    // =========================================================
    private HashMap<Integer, int[]> detectarItypeDirecciones(StateObservation so) {
        HashMap<Integer, int[]> result = new HashMap<>();
        HashSet<Integer> catItypes = new HashSet<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null)
            for (ArrayList<Observation> lista : inmov)
                for (Observation obs : lista) {
                    int it = obs.itype;
                    if (it != 0 && it != 2 && it != 3 && it != 18)
                        catItypes.add(it);
                }
        for (int itype : catItypes) {
            int[] dir = detectarDirParaItype(so, itype);
            if (dir != null) result.put(itype, dir);
        }
        return result;
    }

    private int[] detectarDirParaItype(StateObservation so, int itype) {
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov == null) return null;
        for (ArrayList<Observation> lista : inmov)
            for (Observation obs : lista) {
                if (obs.itype != itype) continue;
                int cx = gx(obs.position), cy = gy(obs.position);
                int[] dir = simularEntradaCatapulta(so, cx, cy);
                if (dir != null) return dir;
            }
        return null;
    }

    private int[] simularEntradaCatapulta(StateObservation so, int catX, int catY) {
        Queue<StateObservation> q = new LinkedList<>();
        HashSet<String> vis = new HashSet<>();
        q.add(so.copy()); vis.add(bfsKey(so));
        ACTIONS[] dirs = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
                          ACTIONS.ACTION_LEFT, ACTIONS.ACTION_DOWN};
        int maxIter = 500;
        while (!q.isEmpty() && maxIter-- > 0) {
            StateObservation s = q.poll();
            if (s.isGameOver() || s.getAvatarType() != 9) continue;
            Vector2d p = s.getAvatarPosition();
            int ax = gx(p), ay = gy(p);
            for (ACTIONS dir : dirs) {
                int[] d = delta(dir);
                if (ax + d[0] == catX && ay + d[1] == catY) {
                    if (s.getAvatarResources().getOrDefault(15, 0) > 0) {
                        StateObservation test = s.copy();
                        test.advance(dir);
                        if (test.isGameOver()) continue;
                        int px = gx(test.getAvatarPosition());
                        int py = gy(test.getAvatarPosition());
                        for (int t = 0; t < 20; t++) {
                            test.advance(ACTIONS.ACTION_NIL);
                            if (test.isGameOver()) break;
                            int nx = gx(test.getAvatarPosition());
                            int ny = gy(test.getAvatarPosition());
                            if (nx != px || ny != py) return new int[]{nx - px, ny - py};
                        }
                    }
                }
            }
            for (ACTIONS dir : dirs) {
                StateObservation child = s.copy();
                child.advance(dir);
                if (child.isGameOver()) continue;
                if (child.getAvatarType() != 9) continue;
                String k = bfsKey(child);
                if (!vis.contains(k)) { vis.add(k); q.add(child); }
            }
        }
        return null;
    }

    private String bfsKey(StateObservation so) {
        Vector2d p = so.getAvatarPosition();
        return gx(p) + "," + gy(p) + "," + so.getAvatarResources().getOrDefault(15, 0);
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
    //  DFS con modelo propio
    // =========================================================
    /**
     * DFS iterativo con Stack. Sin heurística.
     * Usa HashMap<String, Integer> de visitados con coste para permitir
     * reencontrar un estado con mejor camino.
     * Orden de expansión: R, U, L, D, NIL (se pushean en orden inverso).
     */
    private ArrayList<ACTIONS> buscarDFS() {
        Stack<Nodo> frontera = new Stack<>();
        HashMap<String, Integer> visitados = new HashMap<>();

        Estado e0 = new Estado(iniX, iniY, 0, false,
            (1<<numMon)-1, (1<<numCats)-1, 0, 0, 0);
        frontera.push(new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, 0));
        Nodo meta = null;

        while (!frontera.isEmpty()) {
            Nodo ac = frontera.pop();
            String ka = ac.e.key();

            // Control de visitados con coste
            Integer prevCoste = visitados.get(ka);
            if (prevCoste != null && prevCoste <= ac.g) continue;
            visitados.put(ka, ac.g);

            nodosExp++;
            if (ac.pr > profMax) profMax = ac.pr;

            // ¿Meta?
            if (esMeta(ac.e)) { meta = ac; break; }

            // Expandir en orden INVERSO para que Stack saque RIGHT primero
            for (int i = ORDEN.length - 1; i >= 0; i--) {
                Estado h = trans(ac.e, ORDEN[i]);
                if (h == null) continue;
                frontera.push(new Nodo(h, ac, ORDEN[i], ac.g + 1, ac.pr + 1));
            }
        }

        // Reconstruir plan
        ArrayList<ACTIONS> r = new ArrayList<>();
        if (meta != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) p.push(n.accion);
            while (!p.isEmpty()) r.add(p.pop());
        }

        // Métricas DFS: nodos expandidos, profundidad máxima, nº acciones plan
        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp);
        mp.setProfundidadMaxima(profMax);
        mp.setNumAccionesPlan(meta != null ? r.size() : -1);
        mp.printMetrics();
        return r;
    }

    private boolean esMeta(Estado e) { return e.x==metaX && e.y==metaY && e.llave && e.fase==0; }

    // =========================================================
    //  TRANSICIÓN — idéntica a AgenteAStar
    // =========================================================
    private Estado trans(Estado e, ACTIONS a) {
        if (e.fase==0) {
            if (a==ACTIONS.ACTION_NIL) return null;
            int[] d=delta(a); int nx=e.x+d[0], ny=e.y+d[1];
            if (nx<0||nx>=gridW||ny<0||ny>=gridH) return null;
            if (muro[nx][ny]) return null;
            if (nx==metaX&&ny==metaY&&!e.llave) return null;
            int m=e.mon; boolean l=e.llave; int mB=e.mB, cB=e.cB;
            int mi=monIdx(nx,ny); if(mi>=0&&(mB&(1<<mi))!=0&&m<5){m++;mB&=~(1<<mi);}
            if(nx==llaveX&&ny==llaveY&&!l) l=true;
            long pk=enc(nx,ny); Integer ci=catIdx.get(pk);
            if(ci!=null&&(cB&(1<<ci))!=0) {
                if(m<=0) return null;
                m--; int[] dir=catDir.get(pk); cB&=~(1<<ci);
                return new Estado(nx,ny,m,l,mB,cB,1,dir[0],dir[1]);
            }
            return new Estado(nx,ny,m,l,mB,cB,0,0,0);
        } else if (e.fase==1) {
            if(a!=ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x,e.y,e.mon,e.llave,e.mB,e.cB,2,e.vdx,e.vdy);
        } else if (e.fase==2) {
            if(a!=ACTIONS.ACTION_NIL) return null;
            int tx=e.x+e.vdx, ty=e.y+e.vdy;
            boolean col=(tx<0||tx>=gridW||ty<0||ty>=gridH);
            if(!col) col=(muro[tx][ty]&&!agua[tx][ty])||(tx==metaX&&ty==metaY&&!e.llave);
            if(col) {
                if (agua[e.x][e.y]) return null;
                return new Estado(e.x,e.y,e.mon,e.llave,e.mB,e.cB,0,0,0);
            }
            int nx=tx,ny=ty,m=e.mon;boolean l=e.llave;int mB=e.mB,cB=e.cB,vx=e.vdx,vy=e.vdy;
            int mi=monIdx(nx,ny);if(mi>=0&&(mB&(1<<mi))!=0&&m<5){m++;mB&=~(1<<mi);}
            if(nx==llaveX&&ny==llaveY&&!l) l=true;
            long pk=enc(nx,ny);Integer ci=catIdx.get(pk);
            if(ci!=null&&(cB&(1<<ci))!=0){
                int[] dir=catDir.get(pk);cB&=~(1<<ci);
                return new Estado(nx,ny,m,l,mB,cB,3,dir[0],dir[1]);
            }
            return new Estado(nx,ny,m,l,mB,cB,2,vx,vy);
        } else if (e.fase==3) {
            if(a!=ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x,e.y,e.mon,e.llave,e.mB,e.cB,2,e.vdx,e.vdy);
        }
        return null;
    }

    // =========================================================
    //  UTILIDADES — idénticas a AgenteAStar
    // =========================================================
    private int gx(Vector2d p) { return (int)(p.x/blockSize); }
    private int gy(Vector2d p) { return (int)(p.y/blockSize); }
    private long enc(int x,int y) { return (long)y*gridW+x; }
    private int[] delta(ACTIONS a) {
        switch(a){case ACTION_RIGHT:return new int[]{1,0};case ACTION_LEFT:return new int[]{-1,0};
        case ACTION_UP:return new int[]{0,-1};case ACTION_DOWN:return new int[]{0,1};default:return new int[]{0,0};}
    }
    private int monIdx(int x,int y) {
        long k=enc(x,y);for(int i=0;i<numMon;i++)if(monPos[i]==k)return i;return -1;
    }

    // =========================================================
    //  ESTADO — idéntico a AgenteAStar
    // =========================================================
    private static class Estado {
        int x,y,mon,mB,cB,fase,vdx,vdy;boolean llave;
        Estado(int x,int y,int m,boolean l,int mB,int cB,int f,int vx,int vy){
            this.x=x;this.y=y;mon=m;llave=l;this.mB=mB;this.cB=cB;fase=f;vdx=vx;vdy=vy;}
        String key(){return x+","+y+","+mon+","+(llave?1:0)+","+mB+","+cB+","+fase+","+vdx+","+vdy;}
    }

    // =========================================================
    //  NODO DFS — sin heurística ni comparación
    // =========================================================
    private static class Nodo {
        Estado e; Nodo padre; ACTIONS accion; int g, pr;
        Nodo(Estado e, Nodo padre, ACTIONS accion, int g, int pr) {
            this.e = e; this.padre = padre; this.accion = accion;
            this.g = g; this.pr = pr;
        }
    }
}