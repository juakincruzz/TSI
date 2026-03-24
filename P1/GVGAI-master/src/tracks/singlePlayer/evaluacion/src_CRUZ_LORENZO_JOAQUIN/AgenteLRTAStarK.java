package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

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
    private int metaX, metaY, iniX, iniY;
    private boolean[][] muro, agua;

    private HashMap<Integer, int[]> itypeToDir;
    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;
    private long[] monPos;
    private int numMon;
    private int llaveX = -1, llaveY = -1;

    // --- Estructuras LRTA*(k) ---
    private HashMap<String, Double> tablaH;
    private HashMap<String, String> soporte; // Guarda el key del mejor vecino
    private HashMap<String, Estado> estadosVisitados; // Para recuperar vecinos hacia atrás

    // Métricas oficiales
    private int nodosExp = 0, numAcciones = 0, numActualizacionesTabla = 0;
    private long tiempoInicio;
    private boolean haTerminado = false;

    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN
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
        estadosVisitados = new HashMap<>();

        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        itypeToDir = detectarItypeDirecciones(so);

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

        tiempoInicio = System.currentTimeMillis();
        System.out.println("LRTA*(k=" + K + ") init: Listo para aprender.");
    }

    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (haTerminado) return ACTIONS.ACTION_NIL;

        if (so.getAvatarType() != 9 || so.getAvatarPosition() == null) {
            nodosExp++; numAcciones++;
            return ACTIONS.ACTION_NIL;
        }

        Estado ac = sincronizarEstado(so);
        estadosVisitados.put(ac.key(), ac);

        if (esMeta(ac)) {
            finalizarBusqueda();
            return ACTIONS.ACTION_NIL;
        }

        nodosExp++; 
        numAcciones++;

        // 1. PROPAGACIÓN Y ACTUALIZACIÓN (LookaheadUpdateK)
        lookaheadUpdateK(ac, K);

        // 2. SELECCIÓN DEL MEJOR VECINO (Regla de Movimiento)
        double mejorF = Double.MAX_VALUE;
        ACTIONS mejorAccion = ACTIONS.ACTION_NIL;

        for (ACTIONS a : ORDEN) {
            Estado vecino = trans(ac, a);
            if (vecino != null) {
                double f = 1.0 + obtenerH(vecino);
                if (f < mejorF) {
                    mejorF = f;
                    mejorAccion = a;
                }
            }
        }

        return mejorAccion;
    }

    // =========================================================
    //  PROPAGACIÓN LRTA*(k)
    // =========================================================
    private void lookaheadUpdateK(Estado inicio, int limiteK) {
        Queue<Estado> cola = new LinkedList<>();
        cola.add(inicio);
        int contador = limiteK - 1;

        while (!cola.isEmpty()) {
            Estado x = cola.poll();
            
            // 1. Calcular el mejor vecino de X
            double minF = Double.MAX_VALUE;
            Estado mejorVecinoX = null;
            
            for (ACTIONS a : ORDEN) {
                Estado vecino = trans(x, a);
                if (vecino != null) {
                    estadosVisitados.put(vecino.key(), vecino); // Registrar para propagación
                    double f = 1.0 + obtenerH(vecino);
                    if (f < minF) {
                        minF = f;
                        mejorVecinoX = vecino;
                    }
                }
            }

            if (mejorVecinoX == null) continue; // Estado sin salida

            // 2. Actualizar el Soporte
            soporte.put(x.key(), mejorVecinoX.key());

            // 3. Regla de Aprendizaje LRTA* (1º mínimo)
            boolean propagar = false;
            double hActual = obtenerH(x);
            
            if (hActual < minF) {
                propagar = true;
                tablaH.put(x.key(), minF);
                numActualizacionesTabla++;
            }

            // 4. Propagación hacia los nodos que dependen de X
            if (propagar && contador > 0) {
                // Buscamos en nuestra memoria qué nodos tenían a X como su "mejor vecino"
                List<Estado> dependientes = obtenerNodosConSoporte(x.key());
                for (Estado sucesor : dependientes) {
                    cola.add(sucesor);
                }
                contador--;
            }
        }
    }

    /**
     * Devuelve una lista de los estados visitados cuyo soporte actual es 'keySoporte'
     */
    private List<Estado> obtenerNodosConSoporte(String keySoporte) {
        List<Estado> dependientes = new ArrayList<>();
        for (String key : soporte.keySet()) {
            if (soporte.get(key).equals(keySoporte)) {
                dependientes.add(estadosVisitados.get(key));
            }
        }
        return dependientes;
    }

    // =========================================================
    //  RESTO DE FUNCIONES (Idénticas a RTA*)
    // =========================================================
    private Estado sincronizarEstado(StateObservation so) {
        Vector2d ap = so.getAvatarPosition();
        int ax = gx(ap), ay = gy(ap);
        int mB = 0; boolean llave = true; 
        
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) { 
                        int mi = monIdx(gx(obs.position), gy(obs.position));
                        if (mi >= 0) mB |= (1 << mi);
                    } else if (obs.itype == 16) { llave = false; }
                }
            }
        }
        int mon = numMon - Integer.bitCount(mB);

        int cB = 0;
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    if (itypeToDir.containsKey(obs.itype)) {
                        long pk = enc(gx(obs.position), gy(obs.position));
                        Integer ci = catIdx.get(pk);
                        if (ci != null) cB |= (1 << ci);
                    }
                }
            }
        }
        return new Estado(ax, ay, mon, llave, mB, cB, 0, 0, 0);
    }

    private double obtenerH(Estado e) {
        String k = e.key();
        if (tablaH.containsKey(k)) return tablaH.get(k);
        double h0 = hM(e.x, e.y); 
        tablaH.put(k, h0);
        return h0;
    }

    private boolean esMeta(Estado e) { return e.x == metaX && e.y == metaY && e.llave; }

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
            
            int m = e.mon; boolean l = e.llave; int mB = e.mB, cB = e.cB;
            
            int mi = monIdx(nx, ny); 
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            if (nx == llaveX && ny == llaveY && !l) l = true;
            
            long pk = enc(nx, ny); 
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                if (m <= 0) return null; 
                m--; int[] dir = catDir.get(pk); cB &= ~(1 << ci);
                int vx = dir[0], vy = dir[1], cx = nx, cy = ny;
                
                while (true) {
                    int tx = cx + vx, ty = cy + vy;
                    boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
                    if (!col) col = (muro[tx][ty] && !agua[tx][ty]) || (tx == metaX && ty == metaY && !l);
                    
                    if (col) {
                        if (agua[cx][cy]) return null; 
                        return new Estado(cx, cy, m, l, mB, cB, 0, 0, 0); 
                    }
                    cx = tx; cy = ty;
                    int miVuelo = monIdx(cx, cy); 
                    if (miVuelo >= 0 && (mB & (1 << miVuelo)) != 0 && m < 5) { m++; mB &= ~(1 << miVuelo); }
                    if (cx == llaveX && cy == llaveY && !l) l = true;
                    
                    long pkVuelo = enc(cx, cy); Integer ciVuelo = catIdx.get(pkVuelo);
                    if (ciVuelo != null && (cB & (1 << ciVuelo)) != 0) {
                        int[] dirVuelo = catDir.get(pkVuelo); cB &= ~(1 << ciVuelo);
                        vx = dirVuelo[0]; vy = dirVuelo[1];
                    }
                }
            }
            return new Estado(nx, ny, m, l, mB, cB, 0, 0, 0);
        }
        return null;
    }

    private double hM(int x, int y) { return Math.abs(x - metaX) + Math.abs(y - metaY); }
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

    private HashMap<Integer, int[]> detectarItypeDirecciones(StateObservation so) {
        HashMap<Integer, int[]> result = new HashMap<>();
        HashSet<Integer> catItypes = new HashSet<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int it = obs.itype;
                    if (it != 0 && it != 2 && it != 3 && it != 18) catItypes.add(it);
                }
            }
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
        for (ArrayList<Observation> lista : inmov) {
            for (Observation obs : lista) {
                if (obs.itype != itype) continue;
                int[] dir = simularEntradaCatapulta(so, gx(obs.position), gy(obs.position));
                if (dir != null) return dir;
            }
        }
        return null;
    }

    private int[] simularEntradaCatapulta(StateObservation so, int catX, int catY) {
        Queue<StateObservation> q = new LinkedList<>();
        HashSet<String> vis = new HashSet<>();
        q.add(so.copy()); vis.add(gx(so.getAvatarPosition()) + "," + gy(so.getAvatarPosition()));
        int maxIter = 500;
        while (!q.isEmpty() && maxIter-- > 0) {
            StateObservation s = q.poll();
            if (s.isGameOver() || s.getAvatarType() != 9) continue;
            Vector2d p = s.getAvatarPosition();
            int ax = gx(p), ay = gy(p);
            for (ACTIONS dir : ORDEN) {
                int[] d = delta(dir);
                if (ax + d[0] == catX && ay + d[1] == catY) {
                    if (s.getAvatarResources().getOrDefault(15, 0) > 0) {
                        StateObservation test = s.copy();
                        test.advance(dir);
                        if (test.isGameOver()) continue;
                        int px = gx(test.getAvatarPosition()), py = gy(test.getAvatarPosition());
                        for (int t = 0; t < 20; t++) {
                            test.advance(ACTIONS.ACTION_NIL);
                            if (test.isGameOver()) break;
                            int nx = gx(test.getAvatarPosition()), ny = gy(test.getAvatarPosition());
                            if (nx != px || ny != py) return new int[]{nx - px, ny - py};
                        }
                    }
                }
            }
            for (ACTIONS dir : ORDEN) {
                StateObservation child = s.copy();
                child.advance(dir);
                if (child.isGameOver() || child.getAvatarType() != 9) continue;
                String k = gx(child.getAvatarPosition()) + "," + gy(child.getAvatarPosition());
                if (!vis.contains(k)) { vis.add(k); q.add(child); }
            }
        }
        return null;
    }

    private static class Estado {
        int x, y, mon, mB, cB, fase, vdx, vdy; boolean llave;
        Estado(int x, int y, int m, boolean l, int mB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }
        String key() { return x+","+y+","+mon+","+(llave?1:0)+","+mB+","+cB; }
    }

    // =========================================================
    //  FIN DE PARTIDA (GVGAI llama a este método al terminar)
    // =========================================================
    @Override
    public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        if (!haTerminado) {
            System.out.println("¡El motor de GVGAI ha detectado el fin de la partida!");
            finalizarBusqueda();
        }
    }
}