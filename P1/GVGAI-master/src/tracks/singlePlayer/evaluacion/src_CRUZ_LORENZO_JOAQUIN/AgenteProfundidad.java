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

    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;
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

    public AgenteProfundidad(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        tipoAvatarNormal = so.getAvatarType();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // Registrar las monedas iniciales para la simulación
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

        catDir = new HashMap<>();
        catIdx = new HashMap<>();

        // === RADAR AUTODIDACTA INCREMENTAL ===
        HashMap<Integer, int[]> itypeToDir = detectarItypeDireccionesIncremental(so);

        System.out.println("\n[RADAR] Catapultas aprendidas:");
        for (Map.Entry<Integer, int[]> entry : itypeToDir.entrySet()) {
            System.out.println(" -> itype " + entry.getKey() + " vuela hacia (" + entry.getValue()[0] + "," + entry.getValue()[1] + ")");
        }

        // === CONSTRUCCIÓN DEL MAPA MATEMÁTICO FINAL ===
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir.clear(); catIdx.clear();
        ArrayList<long[]> catList = new ArrayList<>();
        
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;
                    if (obs.itype == 0) muro[x][y] = true;
                    else if (obs.itype == 3) { agua[x][y] = true; muro[x][y] = true; }
                    else if (itypeToDir.containsKey(obs.itype)) {
                        long pk = enc(x, y);
                        catDir.put(pk, itypeToDir.get(obs.itype));
                        catList.add(new long[]{pk});
                        muro[x][y] = false; // Convierte el muro de agua en catapulta pisable
                    }
                }
            }
        }
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        System.out.println("DFS init: " + gridW + "x" + gridH + " meta=(" + metaX + "," + metaY + ") cats=" + numCats + "\n");
    }

    /**
     * El Radar aprende iterativamente. Usa su imaginación matemática para cruzar islas 
     * ya descubiertas y alcanzar nuevas catapultas desconocidas.
     */
    private HashMap<Integer, int[]> detectarItypeDireccionesIncremental(StateObservation so) {
        HashMap<Integer, int[]> knownDirs = new HashMap<>();
        HashSet<Integer> targetItypes = new HashSet<>();
        int[][] mapItype = new int[gridW][gridH];
        for (int i = 0; i < gridW; i++) Arrays.fill(mapItype[i], -1);

        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int it = obs.itype;
                    int x = gx(obs.position), y = gy(obs.position);
                    if (x>=0 && x<gridW && y>=0 && y<gridH) mapItype[x][y] = it;
                    if (it != 0 && it != 2 && it != 3 && it != 18) {
                        targetItypes.add(it); // Posible catapulta
                    }
                }
            }
        }

        boolean learnedNew = true;
        while (learnedNew && knownDirs.size() < targetItypes.size()) {
            learnedNew = false;
            
            // Reconstruye el mundo mental con las catapultas que ya conoce
            muro = new boolean[gridW][gridH];
            agua = new boolean[gridW][gridH];
            catDir.clear(); catIdx.clear();
            ArrayList<long[]> catList = new ArrayList<>();
            
            if (inmov != null) {
                for (ArrayList<Observation> lista : inmov) {
                    for (Observation obs : lista) {
                        int x = gx(obs.position), y = gy(obs.position);
                        if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;
                        if (obs.itype == 0) muro[x][y] = true;
                        else if (obs.itype == 3) { agua[x][y] = true; muro[x][y] = true; }
                        else if (knownDirs.containsKey(obs.itype)) {
                            long pk = enc(x, y);
                            catDir.put(pk, knownDirs.get(obs.itype));
                            catList.add(new long[]{pk});
                            muro[x][y] = false; 
                        }
                    }
                }
            }
            int ci = 0;
            for (long[] cl : catList) catIdx.put(cl[0], ci++);
            numCats = ci;

            // Lanza una onda mental (BFS) para buscar la catapulta más cercana que AÚN NO conoce
            Queue<Nodo> q = new LinkedList<>();
            HashSet<String> vis = new HashSet<>();
            
            boolean tieneLlaveInicial = (llaveX == -1);
            Estado e0 = new Estado(iniX, iniY, 0, tieneLlaveInicial, (1<<numMon)-1, (1<<numCats)-1, 0, 0, 0);
            q.add(new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, 0));
            vis.add(e0.key());
            
            Nodo foundNode = null;
            ACTIONS enterAction = null;
            int foundItype = -1;

            while (!q.isEmpty()) {
                Nodo ac = q.poll();
                Estado e = ac.e;

                // Comprueba si está a 1 casilla de una catapulta misteriosa y TIENE moneda para pagar
                if (e.fase == 0 && e.mon > 0) { 
                    boolean found = false;
                    for (ACTIONS a : ORDEN) {
                        if (a == ACTIONS.ACTION_NIL) continue;
                        int[] d = delta(a);
                        int nx = e.x + d[0], ny = e.y + d[1];
                        if (nx>=0 && nx<gridW && ny>=0 && ny<gridH) {
                            int it = mapItype[nx][ny];
                            if (targetItypes.contains(it) && !knownDirs.containsKey(it)) {
                                foundNode = ac;
                                enterAction = a;
                                foundItype = it;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }

                // Genera futuros matemáticos a velocidad luz
                for (ACTIONS a : ORDEN) {
                    Estado h = trans(ac.e, a);
                    if (h == null) continue;
                    String hk = h.key();
                    if (!vis.contains(hk)) {
                        vis.add(hk);
                        q.add(new Nodo(h, ac, a, ac.g + 1, ac.pr + 1));
                    }
                }
            }

            // Si logró imaginar un camino hasta la catapulta desconocida, lo ejecuta de verdad
            if (foundNode != null) {
                ArrayList<ACTIONS> path = new ArrayList<>();
                Nodo n = foundNode;
                while (n.padre != null) {
                    path.add(0, n.accion);
                    n = n.padre;
                }
                
                StateObservation sim = so.copy();
                for (ACTIONS a : path) sim.advance(a);
                
                sim.advance(enterAction); // ¡Salta a la catapulta!
                if (!sim.isGameOver()) {
                    int px = gx(sim.getAvatarPosition()), py = gy(sim.getAvatarPosition());
                    int[] detectedDir = null;
                    for (int t = 0; t < 15; t++) {
                        sim.advance(ACTIONS.ACTION_NIL); // Simula el vuelo
                        if (sim.isGameOver()) break;
                        Vector2d pos = sim.getAvatarPosition();
                        if (pos != null && sim.getAvatarType() != tipoAvatarNormal) { 
                            int nx = gx(pos), ny = gy(pos);
                            if (nx != px || ny != py) {
                                detectedDir = new int[]{nx - px, ny - py};
                                // Normaliza la física matemática
                                if (detectedDir[0] > 0) detectedDir[0] = 1;
                                if (detectedDir[0] < 0) detectedDir[0] = -1;
                                if (detectedDir[1] > 0) detectedDir[1] = 1;
                                if (detectedDir[1] < 0) detectedDir[1] = -1;
                                break;
                            }
                        }
                    }
                    if (detectedDir != null) {
                        knownDirs.put(foundItype, detectedDir); // ¡Aprendido!
                        learnedNew = true; // Reinicia el ciclo para buscar la siguiente
                    } else {
                        targetItypes.remove(foundItype); // Falsa alarma, era un muro raro
                        learnedNew = true; 
                    }
                } else {
                    targetItypes.remove(foundItype);
                    learnedNew = true;
                }
            } else {
                break; // Ya no puede alcanzar más islas
            }
        }
        return knownDirs;
    }

    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) plan = buscarDFS();
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    private ArrayList<ACTIONS> buscarDFS() {
        Stack<Nodo> frontera = new Stack<>();
        HashMap<String, Integer> visitados = new HashMap<>();

        boolean tieneLlaveInicial = (llaveX == -1);
        Estado e0 = new Estado(iniX, iniY, 0, tieneLlaveInicial, (1<<numMon)-1, (1<<numCats)-1, 0, 0, 0);
        frontera.push(new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, 0));
        Nodo meta = null;

        while (!frontera.isEmpty()) {
            Nodo ac = frontera.pop();
            String ka = ac.e.key();

            Integer prevCoste = visitados.get(ka);
            if (prevCoste != null && prevCoste <= ac.g) continue;
            visitados.put(ka, ac.g);

            nodosExp++;
            if (ac.pr > profMax) profMax = ac.pr;

            if (esMeta(ac.e)) { meta = ac; break; }

            for (int i = ORDEN.length - 1; i >= 0; i--) {
                Estado h = trans(ac.e, ORDEN[i]);
                if (h == null) continue;
                frontera.push(new Nodo(h, ac, ORDEN[i], ac.g + 1, ac.pr + 1));
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

    private boolean esMeta(Estado e) { return e.x==metaX && e.y==metaY && e.llave && e.fase==0; }

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

    private static class Nodo {
        Estado e; Nodo padre; ACTIONS accion; int g, pr;
        Nodo(Estado e, Nodo padre, ACTIONS accion, int g, int pr) {
            this.e = e; this.padre = padre; this.accion = accion;
            this.g = g; this.pr = pr;
        }
    }
}