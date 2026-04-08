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
    * Agente de búsqueda en profundidad (DFS)
    * 
    * Estrategia: Búsqueda offline completa antes de actuar.
    * En el primer tick ejecuto el DFS recursivo sobre un modelo propio del
    * estado del juego. El resultado es un plan de acciones que se ejecuta
    * acción a acción en los ticks siguientes.
    * 
    * Modelo de estado: posición (x, y), monedas recogidas (bitmask), llave obtenida,
    * llaves recogidas (bitmask), catapultas usadas (bitmask), y fase de vuelo (0 = normal,
    * 1-3 = fases de catapulta).
    * 
    * Orden de expansión: RIGHT, UP, LEFT, DOWN.
    * En fases de catapulta solo se permite ACTION_NIL.
*/
public class AgenteProfundidad extends AbstractPlayer {
    // --- Dimensiones del mapa ---
    private int blockSize, gridW, gridH;

    // --- Posiciones relevantes ---
    private int metaX, metaY;   // Coordenadas del portal.
    private int iniX, iniY;     // Posición inicial del avatar.

    // --- Obstáculos ---
    private boolean[][] muro;   // 'true' Si la celda es muro o agua.
    private boolean[][] agua;   // 'true' Si la celda es agua (necesario para catapultas)

    // --- Catapultas ---
    // catDir: (x, y) -> Vector de dirección de lanzamiento.
    // catIdx: )x, y) -> Índice en el bitmask de catapultas.
    private HashMap<Long, int[]> catDir;
    private HashMap<Long, Integer> catIdx;
    private int numCats;    // Número total de catapultas en el mapa.

    // --- Monedas ---
    private long[] monPos;  // Posiciones de las monedas.
    private int numMon;     // Número total de monedas.

    // --- Llaves ---
    private long[] llavePos;    // Posiciones codificadas de cada llave.
    private int numLlaves;      // Número total de llaves.

    // --- Plan y métricas ---
    // plan: lista de acciones a ejecutar, generada de una vez por el DFS.
    private ArrayList<ACTIONS> plan = null;
    private int nodosExp = 0;   // Nodos expandidos durante la búsqueda.
    private int profMax = 0;    // Profundidad máxima alcanzada en el árbol DFS.

    /**
        * Orden de expansión de acciones en fase normal.
        * RIGHT, UP, LEFT, DOWN garantiza un comportamiento determinista y coherente
        * con el pseudocódigo de las diapositivas.
    */
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, 
        ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  
        ACTIONS.ACTION_DOWN
    };

    /**
        * Devuelve el vector de desplazamiento de una catapulta según el itype.
        * Los itypes 5-8 corresponden a las cuatro direcciones posibles en el juego.
        * Devuelve null si el itype no es de catapulta.
        * @param itype
        * @return Vector de desplazamiento o null.
    */
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

    /**
        * Inicializo el agente extrayendo la información estática del mapa 
        * desde el estado inicial: dimensiones, muros, agua, catapultas, monedas, llaves y portal.
        * Esta información se almacena en estructuras propias para no depender del estado del juego durante la búsqueda.
        * @param so
        * @param timer
    */
    public AgenteProfundidad(StateObservation so, ElapsedCpuTimer timer) {
        super();
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];
        catDir = new HashMap<>();
        catIdx = new HashMap<>();

        // --- Posición inicial del avatar ---
        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // --- Posición del portal (objetivo) ---
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // --- Clasificar inmovables: muros (0), agua (3) y catapultas (5-8) ---
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
                        // El agua es un obstáculo para caminar, pero no para volar.
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

        // Asignar un índice a cada catapulta para el bitmask.
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        // --- Clasificar recursos: monedas (15) y llaves (16) ---
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
    }


    // =========================================================
    //  ACT
    // =========================================================

    /**
        * En el primer tick ejecuto el DFS completa y construyo el plan.
        * En los ticks siguientes, simplemente devuelvo y elimino la primera
        * acción pendiente del plan. Cuando el plan se agota, devuelvo ACTION_NIL.
        * 
        * Esta separación entre planificación y ejecución es lo importante del DFS
        * ya que toda la exploración ocurre antes de mover el avatar.
        * 
        * @param so
        * @param timer
        * @return Acción a ejecutar en este tick.
    */
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) {
            long t0 = System.currentTimeMillis();
            plan = buscarDFS();
            
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setProfundidadMaxima(profMax);
            mp.setNumAccionesPlan(plan.size() > 0 ? plan.size() : -1);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("Profundidad");
            mp.printMetrics();
        }

        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  DFS RECURSIVO (pseudocódigo diapositiva pág.14)
    //
    //  DFS(Nodo inicial, Nodo objetivo):
    //      estado[inicial] = VISITADO
    //      padre[inicial] = null
    //      DFS_search(inicial, objetivo)
    //
    //  DFS_search(u, objetivo):
    //      if u == objetivo: return TRUE       <- u es nodo expandido
    //      for each v in sucesores(u):
    //          if estado[v] == NOVISITADO:
    //              estado[v] = VISITADO
    //              padre[v] = u
    //              if DFS_search(v, objetivo): return TRUE
    //      return FALSE
    // =========================================================

    // Estructuras compartidas entre buscarDFS() y dfsSearch() para
    // evitar pasar múltiples parámetros en cada llamada recursiva.
    private HashSet<String> visitados;
    private HashMap<String, String> padreKey;       // key hijo -> key padre.
    private HashMap<String, ACTIONS> padreAccion;   // key hijo -> acción que llevó del padre al hijo.
    private String metaKey;                         // key del nodo meta encontrado.

    /**
        *  Lanzo el DFS desde el estado inicial y reconstruyo el plan de acciones 
        *  siguiendo los punteros padre desde el nodo meta hasta el nodo inicial.
        * 
        *  El estado inicial codifica todas las monedas, llaves y catapultas como
        *  disponibles usando bitmask (esto quiere decir todos los bits a 1).
        *  
        * @return Lista ordenada de acciones desde inicio hasta meta o lista vacía si no encuentro solución.
    */
    private ArrayList<ACTIONS> buscarDFS() {
        visitados = new HashSet<>();
        padreKey = new HashMap<>();
        padreAccion = new HashMap<>();
        metaKey = null;

        // Estado inicial: Posición de inicio, sin monedas ni llaves recogidas,
        // todas las monedas, llaves y catapultas disponibles.
        Nodo n0 = new Nodo(iniX, iniY, 0, false,
                (1 << numMon) - 1, 
                (1 << numLlaves) - 1, 
                (1 << numCats) - 1, 
                0, 0, 0);

        String k0 = n0.key();
        visitados.add(k0);
        padreKey.put(k0, null);     // El nodo inicial no tiene padre.

        dfsSearch(n0);

        // Reconstruyo el plan siguiendo los punteros padre desde la meta.
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
        return r;
    }

    /**
        * Implementación recursiva del DFS. 
        * Cada llamada que hago corresponde a la expansión de un nodo 'u'.
        * Se actualiza la profundidad máxima y se incrementa nodosExp antes de
        * generar los sucesores (el nodo meta NO lo expando, solo lo detecto).
        * 
        * En fase normal (0) genero sucesores en el orden RIGHT, UP, LEFT, DOWN
        * En fases de catapulta (1, 2, 3) el único sucesor posible es ACTION_NIL
        * ya que el agente está volando y no puede elegir otra acción.
        
        * @param u
        * @return 'true' Si encuentro la meta en esta rama, 'false' en caso contrario.
    */
    private boolean dfsSearch(Nodo u) {
        String uk = u.key();

        // Calculo la profundidad del nodo actual contando saltos hasta la raíz.
        int depth = 0;
        String k = uk;

        while (padreKey.get(k) != null) { depth++; k = padreKey.get(k); }

        if (depth > profMax) profMax = depth;

        // Si 'u' es la meta, lo que hago es detener la búsqueda sin expandirlo.
        if (esMeta(u)) {
            metaKey = uk;
            return true;
        }

        // El nodo 'u' se expande aquí, incremento el contador.
        nodosExp++;

        if (u.fase == 0) {
            // Fase normal: Pruebo las 4 direcciones en el orden definido en ORDEN.
            for (ACTIONS a : ORDEN) {
                Nodo v = trans(u, a);

                if (v == null) continue;        // Acción no válida.

                String vk = v.key();

                if (!visitados.contains(vk)) {
                    visitados.add(vk);
                    padreKey.put(vk, uk);
                    padreAccion.put(vk, a);
                    if (dfsSearch(v)) return true;
                }
            }
        } else {
            // Fases de catapulta (1, 2, 3): Agente vuela, solo ACTION_NIL.
            Nodo v = trans(u, ACTIONS.ACTION_NIL);

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

    /**
        * Condición de meta: El agente está en el portal, tiene al menos una llave y 
        * se encuentra en fase normal (es decir, no está volando).
        * @param e
        * @return 'true' si 'e' es un estado meta, 'false' en caso contrario.
    */
    private boolean esMeta(Nodo n) {
        return n.x == metaX && n.y == metaY && n.llave && n.fase == 0;
    }

    /**
        * Función de transición: Calculo el estado resultante de aplicar la acción 'a' 
        * al estado 'e' sin modificar 'e'.
        * 
        * Gestiono 4 fases:
        * - Fase 0: Movimiento normal, recogida de objetos, entrada en catapulta.
        * - Fase 1: Tick de inicio de vuelo (posición fija, preparación).
        * - Fase 2: Tick de vuelo activo, el agente avanza según la dirección de la catapulta.
        * - Fase 3: Tick de rebote en catapulta encadenada durante el vuelo.
        * 
        * @param e
        * @param a
        * @return 'null' si el movimiento es inválido (muro, fuera del mapa, etc).
    */
    private Nodo trans(Nodo n, ACTIONS a) {
        if (n.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = n.x + d[0], ny = n.y + d[1];

            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;

            if (muro[nx][ny]) return null;

            int m = n.mon; 
            int mB = n.mB, cB = n.cB, lB = n.lB;
            boolean l = n.llave; 

            // Recojo llave si la hay en la celda destino y aún no la tengo.
            int li = llaveIdx(nx, ny);

            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { 
                lB &= ~(1 << li); 

                l = true; 
            }

            // Recojo moneda si la hay (máximo puedo coger 5 monedas).
            int mi = monIdx(nx, ny);

            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { 
                m++; 

                mB &= ~(1 << mi); 
            }

            // Recojo llave duplicada (marca como recogida aunque ya tenga).
            int llaveIdx = llaveIdx(nx, ny);

            if (llaveIdx >= 0 && (lB & (1 << llaveIdx)) != 0) {
                lB &= ~(1 << llaveIdx);

                l = true;               
            }

            // Entro en catapulta: requiere moneda
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);

            if (ci != null && (cB & (1 << ci)) != 0) {
                if (m <= 0) return null ;

                m--;

                int[] dir = catDir.get(pk);

                cB &= ~(1 << ci);

                // Paso a fase 1: Inicio de vuelo con posición 'dir'.
                return new Nodo(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }

            return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        } else if (n.fase == 1) {
            // Tick de preparación de vuelo: El agente se queda en la misma celda.
            if (a != ACTIONS.ACTION_NIL) return null;

            // Paso a fase 2.
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);

        } else if (n.fase == 2) {
            // Tick de vuelo: El agente avanza según la dirección de la catapulta.
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = n.x + n.vdx, ty = n.y + n.vdy;

            // Colisión: Fuera del mapa, muro o portal sin llave.
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);

            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);

            if (!col && tx == metaX && ty == metaY && !n.llave) col = true;

            if (col) {
                // El agente cae donde está, si es agua, no vale.
                if (agua[n.x][n.y]) return null;
                return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 0, 0, 0);
            }

            int nx = tx, ny = ty;
            int m = n.mon; 
            int mB = n.mB, cB = n.cB, lB = n.lB;
            boolean l = n.llave; 

            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }

            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }

            // Atterizo en el portal con llave: Vuelvo a fase normal.
            if (nx == metaX && ny == metaY && l) {
                return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            }

            // Catapulta encadenada durante el vuelo: Paso a fase 3.
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);

            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);

                cB &= ~(1 << ci);

                return new Nodo(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }

            return new Nodo(nx, ny, m, l, mB, lB, cB, 2, n.vdx, n.vdy);

        } else if (n.fase == 3) {
            // Tick de rebote: Reemplazo la dirección de vuelo con la de la nueva catapulta.
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);
        }
        return null;
    }


    // =========================================================
    //  UTILIDADES
    // =========================================================

    /**
        * Convierto coordenada de píxel a coordenada de celda en el eje X.
        * @param p
        * @return Coordenada de celda X.
    */
    private int gx(Vector2d p) { return (int)(p.x / blockSize); }

    /**
        * Convierto coordenada de píxel a coordenada de celda en el eje Y.
        * @param p
        * @return Coordenada de celda Y.
    */
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }

    /**
        * Codifico (x, y) como un long para usarlo como clave en HashMaps.
        * Evito crear objetos Point o String innecesarios durante la búsqueda.
        * @param x
        * @param y
        * @return Clave única para la posición (x, y).
    */
    private long enc(int x, int y) { return (long)y * gridW + x; }

    /**
        * Devuelvo el vector de desplazamiento (dx, dy) asociado a una acción.
        * @param a
        * @return Vector de desplazamiento o {0, 0} si la acción no es de movimiento.
    */
    private int[] delta(ACTIONS a) {
        switch (a) {
            case ACTION_RIGHT: return new int[]{1, 0};
            case ACTION_LEFT:  return new int[]{-1, 0};
            case ACTION_UP:    return new int[]{0, -1};
            case ACTION_DOWN:  return new int[]{0, 1};
            default:           return new int[]{0, 0};
        }
    }

    /**
        * Devuelvo el índice de la moneda en la posición (x, y) o -1 si no hay moneda.
        * @param x
        * @param y
        * @return Índice de la moneda o -1 si no hay moneda.
    */
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numMon; i++) if (monPos[i] == k) return i;
        return -1;
    }

    /**
        * Devuelvo el índice de la llave en la posición (x, y) o -1 si no hay llave.
        * @param x
        * @param y
        * @return Índice de la llave o -1 si no hay llave.
    */
    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }


    // =========================================================
    //  CLASE INTERNA: NODO
    // =========================================================

    /**
        * Represento un nodo del juego de forma explícita con la información relevante.
        * 
        * Campos:
        * - (x, y): Posición del avatar en coordenadas de celda.
        * - mon: Número de monedas recogidas (0 a 5).
        * - llave: 'true' si el avatar tiene al menos una llave, 'false' en caso contrario.
        * - mB: Bitmask de monedas disponibles en el mapa (1 = moneda disponible, 0 = moneda recogida).
        * - lB: Bitmask de llaves disponibles en el mapa (1 = llave disponible, 0 = llave recogida).
        * - cB: Bitmask de catapultas disponibles en el mapa (1 = catapulta sin usar, 0 = catapulta usada).
        * - fase: fase de vuelo: 0 = normal, 1 = inicio de vuelo, 2 = vuelo activo, 3 = rebote en catapulta encadenada.
        * - vdx, vdy: Vector de desplazamiento de la catapulta actual (solo relevante en fases 1, 2 y 3).
        * 
        * Con el método 'key()' genero una cadena única que identifica el estado, usado como clave en los HashMaps de 
        * visitados, de padre y de acción.
    */
    private static class Nodo {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        Nodo(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }

        /**
            * Genero una clave String que identifica el estado.
            * Incluyo todos los campos relevantes para evitar colisiones entre estados distintos 
            * que comparten posición pero son distintos en recursos o fase de vuelo.
            * @return Clave única para este estado.
        */
        String key() {
            return x + "," + y + "," + mon + "," + (llave ? 1 : 0) + ","
                 + mB + "," + lB + "," + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }
}