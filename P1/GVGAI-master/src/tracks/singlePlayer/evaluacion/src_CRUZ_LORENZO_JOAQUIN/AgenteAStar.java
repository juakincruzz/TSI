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
 * Modelo propio de estado. Búsqueda instantánea en act().
 *
 * Catapultas detectadas por itype desde el VGDL:
 *   Cada subtipo de catapulta tiene un itype diferente.
 *   La dirección se obtiene haciendo advance() de prueba en el constructor
 *   con UNA catapulta de cada itype, y mapeando itype → dirección.
 *
 * Fases: 0=tierra, 1=transformación, 2=vuelo, 3=aterrizaje
 * Heurística: Manhattan al portal.
 * Orden expansión: R, U, L, D, NIL.
 */
public class AgenteAStar extends AbstractPlayer {

    private int blockSize, gridW, gridH;
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    // Catapultas: todas (posición → {dx,dy})
    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;

    // Monedas
    private long[] monPos;
    private int numMon;
    private int llaveX = -1, llaveY = -1;

    private ArrayList<ACTIONS> plan = null;
    private int nodosExp = 0, profMax = 0;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN,
        ACTIONS.ACTION_NIL
    };

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

        // Paso 1: Detectar mapa itype → dirección de catapulta
        // Las catapultas tienen diferentes itypes según su orientación en VGDL.
        // Hacemos advance() de prueba para mapear cada itype de catapulta a su dirección.
        HashMap<Integer, int[]> itypeToDir = detectarItypeDirecciones(so);

        // Paso 2: Clasificar inmovables
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        ArrayList<long[]> catList = new ArrayList<>();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;

                    if (obs.itype == 0) {
                        muro[x][y] = true;  // muro/planta
                    } else if (obs.itype == 3) {
                        agua[x][y] = true;
                        muro[x][y] = true;  // agua = muro para caminar
                    } else if (itypeToDir.containsKey(obs.itype)) {
                        // Es una catapulta
                        int[] dir = itypeToDir.get(obs.itype);
                        long pk = enc(x, y);
                        catDir.put(pk, dir);
                        catList.add(new long[]{pk});
                    }
                    // itype=2 (floor): ignorar
                    // itype=18 (portal): no bloquea
                }
            }
        }

        // Asignar índices a catapultas
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        // Monedas y llave
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

        // Debug
        System.out.println("A* init: " + gridW + "x" + gridH
            + " meta=(" + metaX + "," + metaY + ")"
            + " cats=" + numCats + " monedas=" + numMon
            + " itypeMap=" + itypeToDir.size() + " entries");
        for (Map.Entry<Integer, int[]> e : itypeToDir.entrySet()) {
            System.out.println("  itype=" + e.getKey() + " → (" + e.getValue()[0] + "," + e.getValue()[1] + ")");
        }
    }

    /**
     * Detecta la relación itype → dirección de catapulta.
     * Recorre el grid del motor buscando catapultas (category=4 = inmovable que no es
     * muro/agua/floor). Para cada itype nuevo, simula la entrada con advance()
     * y observa la dirección de vuelo.
     */
    private HashMap<Integer, int[]> detectarItypeDirecciones(StateObservation so) {
        HashMap<Integer, int[]> result = new HashMap<>();

        // Recopilar todos los itypes de catapultas (los que no son muro=0, agua=3, floor=2, portal=18)
        HashSet<Integer> catItypes = new HashSet<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int it = obs.itype;
                    if (it != 0 && it != 2 && it != 3 && it != 18) {
                        catItypes.add(it);
                    }
                }
            }
        }

        // Para cada itype de catapulta, buscar UNA que sea accesible desde
        // una celda adyacente libre, y simular la entrada
        for (int itype : catItypes) {
            int[] dir = detectarDirParaItype(so, itype);
            if (dir != null) {
                result.put(itype, dir);
            }
        }

        return result;
    }

    /**
     * Para un itype dado, busca una catapulta de ese tipo y simula
     * la entrada para detectar la dirección.
     */
    private int[] detectarDirParaItype(StateObservation so, int itype) {
        // Buscar posiciones de catapultas con este itype
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov == null) return null;

        for (ArrayList<Observation> lista : inmov) {
            for (Observation obs : lista) {
                if (obs.itype != itype) continue;
                int cx = gx(obs.position), cy = gy(obs.position);

                // Intentar simular la entrada desde el estado inicial
                // Hacemos: navegar avatar → celda adyacente → entrar → ver vuelo
                // Simplificación: usar BFS con advance para llegar adyacente
                int[] dir = simularEntradaCatapulta(so, cx, cy);
                if (dir != null) return dir;
            }
        }
        return null;
    }

    /**
     * Simula la entrada a una catapulta en (cx,cy) usando BFS con advance()
     * desde el estado inicial. Devuelve la dirección de vuelo detectada.
     */
    private int[] simularEntradaCatapulta(StateObservation so, int catX, int catY) {
        // BFS con advance para llegar adyacente a la catapulta
        Queue<StateObservation> q = new LinkedList<>();
        HashSet<String> vis = new HashSet<>();
        q.add(so.copy());
        vis.add(bfsKey(so));

        ACTIONS[] dirs = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
                          ACTIONS.ACTION_LEFT, ACTIONS.ACTION_DOWN};

        int maxIter = 500;
        while (!q.isEmpty() && maxIter-- > 0) {
            StateObservation s = q.poll();
            if (s.isGameOver() || s.getAvatarType() != 9) continue;

            Vector2d p = s.getAvatarPosition();
            int ax = gx(p), ay = gy(p);

            // ¿Estamos adyacentes a la catapulta?
            for (ACTIONS dir : dirs) {
                int[] d = delta(dir);
                if (ax + d[0] == catX && ay + d[1] == catY) {
                    // ¡Sí! Necesitamos moneda para entrar
                    if (s.getAvatarResources().getOrDefault(15, 0) > 0) {
                        // Entrar a la catapulta
                        StateObservation test = s.copy();
                        test.advance(dir);
                        if (test.isGameOver()) continue;

                        // Ver dirección de vuelo: hacer NILs hasta que se mueva
                        int px = gx(test.getAvatarPosition());
                        int py = gy(test.getAvatarPosition());
                        for (int t = 0; t < 20; t++) {
                            test.advance(ACTIONS.ACTION_NIL);
                            if (test.isGameOver()) break;
                            int nx = gx(test.getAvatarPosition());
                            int ny = gy(test.getAvatarPosition());
                            if (nx != px || ny != py) {
                                return new int[]{nx - px, ny - py};
                            }
                        }
                    }
                }
            }

            // Expandir
            for (ACTIONS dir : dirs) {
                StateObservation child = s.copy();
                child.advance(dir);
                if (child.isGameOver()) continue;
                if (child.getAvatarType() != 9) continue; // Evitar pisar catapultas accidentalmente
                String k = bfsKey(child);
                if (!vis.contains(k)) {
                    vis.add(k);
                    q.add(child);
                }
            }
        }
        return null; // No pudimos llegar
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
        if (plan == null) plan = buscarAStar();
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  A* con modelo propio
    // =========================================================
    private ArrayList<ACTIONS> buscarAStar() {
        PriorityQueue<Nodo> ab = new PriorityQueue<>();
        HashMap<String, Nodo> abM = new HashMap<>();
        HashMap<String, Nodo> ce = new HashMap<>();
        int ord = 0;

        Estado e0 = new Estado(iniX, iniY, 0, false,
            (1<<numMon)-1, (1<<numCats)-1, 0, 0, 0);
        Nodo n0 = new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, hM(iniX, iniY), 0, ord++);
        ab.add(n0); abM.put(e0.key(), n0);
        Nodo meta = null;

        while (!ab.isEmpty()) {
            Nodo ac = ab.poll();
            if (ac.obs) continue;
            String ka = ac.e.key();
            if (abM.get(ka) != ac) continue;
            abM.remove(ka);
            nodosExp++; if (ac.pr > profMax) profMax = ac.pr;
            if (esMeta(ac.e)) { meta = ac; break; }
            ce.put(ka, ac);

            for (ACTIONS a : ORDEN) {
                Estado h = trans(ac.e, a);
                if (h == null) continue;
                int gN = ac.g + 1; double hN = hM(h.x, h.y); String kS = h.key();
                if (esMeta(h)) { meta = new Nodo(h,ac,a,gN,0,ac.pr+1,ord++); break; }
                Nodo eC = ce.get(kS);
                if (eC!=null) { if(gN<eC.g){ce.remove(kS);Nodo ns=new Nodo(h,ac,a,gN,hN,ac.pr+1,ord++);ab.add(ns);abM.put(kS,ns);}continue; }
                Nodo eA = abM.get(kS);
                if (eA==null) { Nodo ns=new Nodo(h,ac,a,gN,hN,ac.pr+1,ord++);ab.add(ns);abM.put(kS,ns); }
                else if (gN<eA.g) { eA.obs=true;Nodo ns=new Nodo(h,ac,a,gN,hN,ac.pr+1,ord++);ab.add(ns);abM.put(kS,ns); }
            }
            if (meta != null) break;
        }

        ArrayList<ACTIONS> r = new ArrayList<>();
        if (meta != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) p.push(n.accion);
            while (!p.isEmpty()) r.add(p.pop());
        }
        MetricsProvider mp = MetricsProvider.getInstance();
        mp.setNodosExpandidos(nodosExp); mp.setProfundidadMaxima(profMax);
        mp.setNodosAbiertos(abM.size()); mp.setNodosCerrados(ce.size());
        mp.setNumAccionesPlan(meta!=null?r.size():-1); mp.printMetrics();
        return r;
    }

    private boolean esMeta(Estado e) { return e.x==metaX && e.y==metaY && e.llave && e.fase==0; }

    // =========================================================
    //  TRANSICIÓN — 4 fases
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
            if(col) return new Estado(e.x,e.y,e.mon,e.llave,e.mB,e.cB,3,0,0);
            int nx=tx,ny=ty,m=e.mon;boolean l=e.llave;int mB=e.mB,cB=e.cB,vx=e.vdx,vy=e.vdy;
            int mi=monIdx(nx,ny);if(mi>=0&&(mB&(1<<mi))!=0&&m<5){m++;mB&=~(1<<mi);}
            if(nx==llaveX&&ny==llaveY&&!l) l=true;
            // Catapulta en vuelo: cambia dirección, consume catapulta, NO cobra moneda
            long pk=enc(nx,ny);Integer ci=catIdx.get(pk);
            if(ci!=null&&(cB&(1<<ci))!=0){int[] dir=catDir.get(pk);vx=dir[0];vy=dir[1];cB&=~(1<<ci);}
            return new Estado(nx,ny,m,l,mB,cB,2,vx,vy);
        } else if (e.fase==3) {
            if(a!=ACTIONS.ACTION_NIL) return null;
            return new Estado(e.x,e.y,e.mon,e.llave,e.mB,e.cB,0,0,0);
        }
        return null;
    }

    private double hM(int x,int y) { return Math.abs(x-metaX)+Math.abs(y-metaY); }
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

    private static class Estado {
        int x,y,mon,mB,cB,fase,vdx,vdy;boolean llave;
        Estado(int x,int y,int m,boolean l,int mB,int cB,int f,int vx,int vy){
            this.x=x;this.y=y;mon=m;llave=l;this.mB=mB;this.cB=cB;fase=f;vdx=vx;vdy=vy;}
        String key(){return x+","+y+","+mon+","+(llave?1:0)+","+mB+","+cB+","+fase+","+vdx+","+vdy;}
    }

    private static class Nodo implements Comparable<Nodo> {
        Estado e;Nodo padre;ACTIONS accion;int g,pr,orden;double h;boolean obs;
        Nodo(Estado e,Nodo p,ACTIONS a,int g,double h,int pr,int o){
            this.e=e;padre=p;accion=a;this.g=g;this.h=h;this.pr=pr;orden=o;}
        double f(){return g+h;}
        @Override public int compareTo(Nodo o){
            double f1=f(),f2=o.f();if(f1!=f2)return Double.compare(f1,f2);
            if(h!=o.h)return Double.compare(h,o.h);return Integer.compare(orden,o.orden);}
    }
}