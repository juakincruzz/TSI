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
 * ============================================================================
 * AGENTE DFS — Búsqueda en profundidad (Depth-First Search)
 * ============================================================================
 *
 * Implementación de búsqueda en profundidad no informada.
 *
 * -------------------------------------------------------------------------
 * QUÉ HACE ESTE AGENTE
 * -------------------------------------------------------------------------
 * Planifica de forma offline (antes de ejecutar ningún movimiento) un camino
 * desde la posición inicial del avatar hasta el portal de salida. Para ello,
 * explora el espacio de estados en profundidad: avanza por una rama hasta
 * encontrar la meta o agotar las opciones, y luego retrocede (backtracking)
 * para probar ramas alternativas.
 *
 * -------------------------------------------------------------------------
 * CÓMO LO HACE — Pseudocódigo (diapositivas del curso, pág. 14)
 * -------------------------------------------------------------------------
 *   DFS(Nodo inicial, Nodo objetivo):
 *       estado[inicial] = VISITADO
 *       padre[inicial] = null
 *       DFS_search(inicial, objetivo)
 *
 *   DFS_search(u, objetivo):
 *       if u == objetivo: return TRUE        // u es nodo expandido
 *       for each v in sucesores(u):
 *           if estado[v] == NOVISITADO:
 *               estado[v] = VISITADO
 *               padre[v] = u
 *               if DFS_search(v, objetivo): return TRUE
 *       return FALSE
 *
 * La búsqueda se implementa de forma recursiva. Cada llamada a dfsSearch(u)
 * corresponde a la expansión de un nodo: primero se comprueba si es meta
 * (sin expandirlo si lo es), y si no lo es, se generan sus sucesores en
 * el orden RIGHT, UP, LEFT, DOWN.
 *
 * -------------------------------------------------------------------------
 * POR QUÉ SE DISEÑA ASÍ
 * -------------------------------------------------------------------------
 * - DFS es una búsqueda NO INFORMADA (no usa heurística). Esto significa
 *   que no tiene preferencia por estados más cercanos a la meta, sino que
 *   explora exhaustivamente rama por rama.
 *
 * - NO GARANTIZA OPTIMALIDAD: el primer camino encontrado no es
 *   necesariamente el más corto. Sin embargo, es COMPLETA cuando se usa
 *   con detección de estados visitados (como aquí), ya que evita ciclos
 *   infinitos y garantiza que, si existe solución, se encontrará.
 *
 * - Se usa un HashSet de estados visitados (por clave de estado completa)
 *   para evitar revisitar el mismo estado. Sin esta detección, DFS podría
 *   entrar en bucles infinitos en el grafo de estados.
 *
 * - La reconstrucción del plan utiliza los mapas padreKey y padreAccion
 *   para trazar el camino desde la meta hasta el inicio, invirtiendo
 *   el orden con una pila (Deque).
 *
 * - Al ser búsqueda offline, calcula el plan completo en la primera
 *   llamada a act() y después simplemente ejecuta las acciones una a una.
 *
 * -------------------------------------------------------------------------
 * DIFERENCIAS CON LOS DEMÁS AGENTES DE LA PRÁCTICA
 * -------------------------------------------------------------------------
 * - A diferencia de A*, no usa función de evaluación f(n) = g(n) + h(n).
 * - A diferencia de RTA* y LRTA*(k), no es en tiempo real: calcula todo
 *   el plan antes de mover el avatar.
 * - Este agente NO implementa el flag catapultasGratis (las catapultas
 *   siempre requieren una moneda), lo cual es correcto para los mapas
 *   de la práctica donde hay monedas disponibles.
 *
 * -------------------------------------------------------------------------
 * CATAPULTAS Y FASES DE VUELO
 * -------------------------------------------------------------------------
 * Detección hardcodeada por itype: 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT.
 * Sistema de 4 fases idéntico a los demás agentes:
 *   - Fase 0: caminando (el agente elige dirección).
 *   - Fase 1: sobre la catapulta (tick de activación).
 *   - Fase 2: en vuelo (avance automático).
 *   - Fase 3: aterrizaje en catapulta encadenada (rebote).
 *
 * -------------------------------------------------------------------------
 * ORDEN DE EXPANSIÓN
 * -------------------------------------------------------------------------
 * RIGHT, UP, LEFT, DOWN — en fase 0.
 * Solo ACTION_NIL — en fases 1-3 (el agente no controla durante el vuelo).
 *
 * @author Joaquín Cruz Lorenzo
 */
public class AgenteProfundidad extends AbstractPlayer {

    // =====================================================================
    //  ATRIBUTOS DEL MAPA (inicializados una vez en el constructor)
    // =====================================================================

    /** Tamaño en píxeles de cada celda de la cuadrícula del juego. */
    private int blockSize;

    /** Dimensiones de la cuadrícula del mapa (ancho x alto en celdas). */
    private int gridW, gridH;

    /** Coordenadas (en celdas) del portal de salida (meta del agente). */
    private int metaX, metaY;

    /** Coordenadas (en celdas) de la posición inicial del avatar. */
    private int iniX, iniY;

    /**
     * Matrices booleanas de obstáculos.
     * muro[x][y] = true si la celda bloquea el paso al caminar (muro sólido o agua).
     * agua[x][y] = true si la celda es agua. Se distingue del muro sólido porque
     * la mecánica de colisión en vuelo difiere: un muro sólido detiene el vuelo y
     * el agente aterriza antes, mientras que caer sobre agua mata al agente.
     */
    private boolean[][] muro;
    private boolean[][] agua;

    /**
     * Mapa de catapultas: posición codificada (long) → vector de lanzamiento {dx, dy}.
     * Se usa HashMap para acceso O(1) por posición.
     */
    private HashMap<Long, int[]> catDir;

    /**
     * Índice bitmask de cada catapulta: posición codificada → índice 0..numCats-1.
     * Este índice determina qué bit del campo cB del Nodo controla cada catapulta.
     */
    private HashMap<Long, Integer> catIdx;

    /** Número total de catapultas en el mapa. */
    private int numCats;

    /** Posiciones codificadas de cada moneda del mapa. */
    private long[] monPos;

    /** Número total de monedas en el mapa. */
    private int numMon;

    /** Posiciones codificadas de cada llave del mapa. */
    private long[] llavePos;

    /** Número total de llaves en el mapa. */
    private int numLlaves;

    // =====================================================================
    //  ATRIBUTOS DE LA BÚSQUEDA
    // =====================================================================

    /** Plan de acciones calculado por DFS (null hasta la primera llamada a act). */
    private ArrayList<ACTIONS> plan = null;

    /** Contador de nodos expandidos (nodos cuyo sucesores se generan). */
    private int nodosExp = 0;

    /** Profundidad máxima alcanzada en el árbol de búsqueda DFS. */
    private int profMax = 0;

    /**
     * Orden fijo de expansión de acciones en fase 0 (caminando).
     * RIGHT, UP, LEFT, DOWN — siguiendo el convenio del pseudocódigo del curso.
     * Este orden determina qué rama explora DFS primero, lo que afecta
     * directamente al camino encontrado (DFS no es óptimo, así que el orden
     * importa para la reproducibilidad frente al solucionario).
     */
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT,
        ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,
        ACTIONS.ACTION_DOWN
    };


    // =====================================================================
    //  DETECCIÓN DE CATAPULTAS POR ITYPE
    // =====================================================================

    /**
     * Devuelve el vector de lanzamiento {dx, dy} según el itype de la catapulta.
     *
     * Correspondencia hardcodeada verificada empíricamente:
     *   itype 5 → DOWN  (0, +1)
     *   itype 6 → UP    (0, -1)
     *   itype 7 → RIGHT (+1, 0)
     *   itype 8 → LEFT  (-1, 0)
     *
     * Se usa hardcoding en lugar de simulación porque advance() en el
     * constructor es lento e inestable.
     *
     * @param itype Tipo del objeto inmóvil según GVGAI.
     * @return Vector {dx, dy} de lanzamiento, o null si no es catapulta.
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


    // =====================================================================
    //  CONSTRUCTOR — Parseo completo del mapa
    // =====================================================================

    /**
     * Constructor del agente DFS.
     *
     * QUÉ HACE: Lee toda la información estática del mapa una sola vez a
     * partir del StateObservation inicial: dimensiones, posición del avatar,
     * portal, muros, agua, catapultas (con dirección e índice bitmask),
     * monedas y llaves.
     *
     * CÓMO LO HACE:
     * 1. Extrae dimensiones de la cuadrícula y posición del avatar.
     * 2. Localiza el portal de salida (meta).
     * 3. Recorre objetos inmóviles clasificándolos por itype:
     *    - itype 0  → muro sólido (bloquea paso).
     *    - itype 3  → agua (bloquea paso y mata en vuelo).
     *    - itype 5-8 → catapultas (se registran con dirección e índice bitmask).
     * 4. Recorre recursos:
     *    - itype 15 → moneda.
     *    - itype 16 → llave.
     *
     * POR QUÉ EN EL CONSTRUCTOR: Toda esta información es estática (el mapa
     * no cambia durante la partida), así que se parsea una sola vez para
     * evitar repetir trabajo durante la búsqueda.
     *
     * @param so    Estado inicial del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU (requerido por la interfaz).
     */
    public AgenteProfundidad(StateObservation so, ElapsedCpuTimer timer) {
        super();

        // --- Dimensiones de la cuadrícula ---
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;

        // --- Matrices de obstáculos ---
        muro = new boolean[gridW][gridH];
        agua = new boolean[gridW][gridH];

        // --- Estructuras para catapultas ---
        catDir = new HashMap<>();
        catIdx = new HashMap<>();

        // --- Posición inicial del avatar (de píxeles a celdas) ---
        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // --- Localizar el portal de salida (meta) ---
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // --- Clasificar objetos inmóviles: muros (itype 0), agua (3) y catapultas (5-8) ---
        ArrayList<long[]> catList = new ArrayList<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();

        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    // Ignorar objetos fuera del mapa (seguridad)
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;

                    if (obs.itype == 0) {
                        muro[x][y] = true;
                    } else if (obs.itype == 3) {
                        // Agua: bloquea el paso al caminar (se marca como muro),
                        // pero se distingue con agua[][] para la lógica de vuelo
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

        // Asignar índice secuencial a cada catapulta para el bitmask
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        // --- Clasificar recursos: monedas (itype 15) y llaves (itype 16) ---
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

        // Convertir listas a arrays para acceso rápido por índice
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);

        numLlaves = kl.size();
        llavePos = new long[numLlaves];
        for (int i = 0; i < numLlaves; i++) llavePos[i] = kl.get(i);
    }


    // =====================================================================
    //  ACT — Punto de entrada del framework GVGAI en cada tick
    // =====================================================================

    /**
     * Método llamado por GVGAI en cada tick del juego.
     *
     * QUÉ HACE:
     * - En la primera llamada: ejecuta el DFS completo, almacena el plan
     *   resultante y registra las métricas (nodos expandidos, profundidad
     *   máxima, longitud del plan y tiempo).
     * - En llamadas posteriores: extrae y devuelve la siguiente acción del plan.
     * - Si el plan se agota: devuelve ACTION_NIL (no hacer nada).
     *
     * POR QUÉ SOLO SE BUSCA UNA VEZ: DFS es una búsqueda offline que
     * calcula el camino completo antes de actuar. Al ser el entorno
     * determinista, el plan precalculado sigue siendo válido durante
     * toda la partida.
     *
     * @param so    Estado actual del juego (no se usa tras la primera llamada).
     * @param timer Temporizador de CPU proporcionado por GVGAI.
     * @return La acción a ejecutar en este tick.
     */
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) {
            // Primera llamada: ejecutar DFS y medir el tiempo
            long t0 = System.currentTimeMillis();
            plan = buscarDFS();

            // Registrar métricas para el solucionario
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setProfundidadMaxima(profMax);
            mp.setNumAccionesPlan(plan.size() > 0 ? plan.size() : -1);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("Profundidad");
            mp.printMetrics();
        }

        // Devolver la siguiente acción del plan, o NIL si ya no quedan
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }


    // =====================================================================
    //  DFS RECURSIVO — Pseudocódigo (diapositivas del curso, pág. 14)
    // =====================================================================
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
    //
    // =====================================================================

    /**
     * Estructuras compartidas entre buscarDFS() y dfsSearch().
     *
     * POR QUÉ SON ATRIBUTOS DE CLASE Y NO PARÁMETROS: La recursión de DFS
     * puede alcanzar gran profundidad. Pasar estas estructuras como
     * parámetros en cada llamada recursiva añadiría overhead innecesario
     * al stack frame de cada invocación. Al ser atributos de clase, todas
     * las llamadas recursivas acceden a las mismas instancias sin coste
     * adicional.
     */

    /**
     * Conjunto de estados ya visitados (por clave String).
     * Usa HashSet para consulta O(1). Evita que DFS revisité el mismo
     * estado, lo que podría causar ciclos infinitos en el grafo de estados.
     */
    private HashSet<String> visitados;

    /**
     * Mapa de padres: clave del hijo → clave del padre.
     * Permite reconstruir el camino desde la meta hasta el inicio
     * trazando los punteros de padre sucesivos.
     */
    private HashMap<String, String> padreKey;

    /**
     * Mapa de acciones: clave del hijo → acción que lleva del padre al hijo.
     * Complementa padreKey para saber qué acción ejecutar en cada paso
     * del plan reconstruido.
     */
    private HashMap<String, ACTIONS> padreAccion;

    /**
     * Clave del nodo meta encontrado (null si no se ha encontrado aún).
     * Se establece en dfsSearch cuando se detecta un estado meta.
     */
    private String metaKey;


    /**
     * Lanza el DFS desde el estado inicial y reconstruye el plan de acciones.
     *
     * QUÉ HACE:
     * 1. Inicializa las estructuras de visitados, padres y acciones.
     * 2. Crea el estado inicial con todos los bitmasks activos (todos los
     *    recursos disponibles).
     * 3. Ejecuta dfsSearch recursivo.
     * 4. Si se encontró la meta, reconstruye el plan siguiendo la cadena
     *    de padres desde la meta hasta el inicio.
     *
     * CÓMO RECONSTRUYE EL PLAN: Recorre padreKey desde metaKey hasta el
     * nodo sin padre (inicio), apilando las acciones correspondientes en
     * una pila (Deque). Luego vacía la pila en una lista, obteniendo el
     * orden correcto (inicio → meta).
     *
     * @return Lista ordenada de acciones desde inicio hasta meta,
     *         o lista vacía si no se encuentra solución.
     */
    private ArrayList<ACTIONS> buscarDFS() {
        visitados = new HashSet<>();
        padreKey = new HashMap<>();
        padreAccion = new HashMap<>();
        metaKey = null;

        // Estado inicial: posición del avatar, 0 monedas, sin llave,
        // todos los bitmasks activos (todos los recursos disponibles),
        // fase 0, sin velocidad de vuelo.
        // (1 << n) - 1 pone los n bits menos significativos a 1.
        Nodo n0 = new Nodo(iniX, iniY, 0, false,
                (1 << numMon) - 1,      // Todas las monedas disponibles
                (1 << numLlaves) - 1,   // Todas las llaves disponibles
                (1 << numCats) - 1,     // Todas las catapultas disponibles
                0, 0, 0);

        String k0 = n0.key();
        visitados.add(k0);          // Marcar inicio como visitado
        padreKey.put(k0, null);     // El nodo inicial no tiene padre

        dfsSearch(n0);

        // --- Reconstruir el plan de acciones ---
        // Se recorre la cadena de padres desde la meta hasta el inicio,
        // apilando acciones y luego vaciando la pila para el orden correcto.
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
     * Implementación recursiva del DFS (DFS_search en el pseudocódigo).
     *
     * QUÉ HACE: Explora recursivamente el espacio de estados en profundidad.
     * Cada llamada corresponde a la expansión de un nodo u:
     * 1. Calcula la profundidad actual (contando saltos hasta la raíz).
     * 2. Comprueba si u es meta → si lo es, registra metaKey y retorna true
     *    SIN expandirlo (el nodo meta se detecta pero no se expanden sus hijos).
     * 3. Si no es meta, incrementa nodosExp y genera los sucesores.
     * 4. Para cada sucesor no visitado, lo marca como visitado, registra
     *    su padre/acción, y recurre. Si la recursión encuentra la meta,
     *    propaga true hacia arriba (backtracking exitoso).
     *
     * POR QUÉ EL NODO META NO SE EXPANDE: Siguiendo el pseudocódigo del
     * curso, la condición "if u == objetivo: return TRUE" se evalúa ANTES
     * de generar sucesores. Esto es coherente con la definición de "nodo
     * expandido" = nodo cuyos sucesores se generan.
     *
     * POR QUÉ SE CALCULA LA PROFUNDIDAD RECORRIENDO PADRES: Aunque podría
     * pasarse como parámetro, se calcula trazando padreKey para mantener
     * consistencia con la estructura de datos existente y evitar añadir
     * un parámetro más a la recursión.
     *
     * ACCIONES SEGÚN LA FASE:
     * - Fase 0 (caminando): se prueban RIGHT, UP, LEFT, DOWN en ese orden.
     * - Fases 1-3 (catapulta/vuelo): solo ACTION_NIL (el agente no controla).
     *
     * @param u Nodo a explorar.
     * @return true si se encontró la meta en esta rama, false si no.
     */
    private boolean dfsSearch(Nodo u) {
        String uk = u.key();

        // Calcular la profundidad del nodo actual contando saltos hasta la raíz
        int depth = 0;
        String k = uk;

        while (padreKey.get(k) != null) { depth++; k = padreKey.get(k); }

        if (depth > profMax) profMax = depth;

        // --- Comprobar si u es meta (sin expandirlo) ---
        // Pseudocódigo: "if u == objetivo: return TRUE"
        if (esMeta(u)) {
            metaKey = uk;
            return true;
        }

        // --- Expandir u: generar sus sucesores ---
        // El nodo se expande aquí (sus hijos se van a generar)
        nodosExp++;

        if (u.fase == 0) {
            // Fase normal: probar las 4 direcciones en el orden R, U, L, D
            for (ACTIONS a : ORDEN) {
                Nodo v = trans(u, a);
                if (v == null) continue;        // Acción inválida (muro, fuera de mapa, etc.)

                String vk = v.key();
                if (!visitados.contains(vk)) {  // Solo explorar estados no visitados
                    visitados.add(vk);
                    padreKey.put(vk, uk);
                    padreAccion.put(vk, a);
                    if (dfsSearch(v)) return true;  // Meta encontrada → propagar éxito
                }
            }
        } else {
            // Fases de catapulta (1, 2, 3): el agente vuela, solo ACTION_NIL
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

        // No se encontró meta en esta rama → backtracking
        return false;
    }


    // =====================================================================
    //  CONDICIÓN DE META
    // =====================================================================

    /**
     * Comprueba si un nodo es estado meta (victoria).
     *
     * QUÉ: El agente gana cuando está en el portal, tiene la llave y se
     * encuentra en fase 0 (caminando, no en vuelo).
     *
     * POR QUÉ SE REQUIERE FASE 0: GVGAI no reconoce victoria si el agente
     * llega al portal en pleno vuelo de catapulta (fases 1-3).
     *
     * @param n Nodo a evaluar.
     * @return true si es estado meta, false en caso contrario.
     */
    private boolean esMeta(Nodo n) {
        return n.x == metaX && n.y == metaY && n.llave && n.fase == 0;
    }


    // =====================================================================
    //  FUNCIÓN DE TRANSICIÓN — Motor de física del juego
    // =====================================================================

    /**
     * Calcula el nodo resultante de aplicar una acción a un nodo dado.
     *
     * QUÉ HACE: Modela fielmente la mecánica del juego, incluyendo movimiento
     * normal, recogida de monedas/llaves, uso de catapultas con coste de
     * monedas, y el sistema completo de vuelo en 4 fases.
     *
     * CÓMO FUNCIONA SEGÚN LA FASE:
     *
     * FASE 0 — CAMINANDO (el agente elige dirección):
     *   - Solo acepta acciones direccionales (no NIL).
     *   - Valida límites del mapa y muros.
     *   - Recoge llaves y monedas (si hay disponibles, máximo 5 monedas).
     *   - Si la nueva celda tiene catapulta disponible: paga 1 moneda
     *     (si no tiene monedas, movimiento inválido), consume la catapulta
     *     y pasa a fase 1.
     *   - Si no hay catapulta, permanece en fase 0.
     *
     * FASE 1 — SOBRE LA CATAPULTA (tick de activación):
     *   - Solo acepta NIL. Transiciona a fase 2 (inicio del vuelo).
     *
     * FASE 2 — EN VUELO (avance automático):
     *   - Avanza una celda en la dirección de lanzamiento.
     *   - Colisión con borde/muro sólido/portal sin llave → aterriza en celda actual.
     *   - Colisión sobre agua → muerte (null).
     *   - Sin colisión: recoge recursos, detecta catapulta encadenada (fase 3)
     *     o sigue volando (fase 2). Si llega al portal con llave → fase 0 (victoria).
     *
     * FASE 3 — REBOTE EN CATAPULTA DURANTE VUELO:
     *   - Solo acepta NIL. Transiciona a fase 2 con la nueva dirección.
     *
     * NOTA: A diferencia de AgenteAStar, este agente NO comprueba si el
     * portal bloquea el paso sin llave en fase 0 (la lógica de recogida
     * de llave + moneda se evalúa antes). El orden de recogida
     * llave → moneda → catapulta es ligeramente distinto al de los otros
     * agentes, pero produce resultados correctos para los mapas de la práctica.
     *
     * @param n Nodo actual.
     * @param a Acción a aplicar.
     * @return Nuevo nodo resultante, o null si la acción es inválida o mortal.
     */
    private Nodo trans(Nodo n, ACTIONS a) {

        // --- FASE 0: Caminando ---
        if (n.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = n.x + d[0], ny = n.y + d[1];

            // Validar límites del mapa
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            // Validar que no sea muro
            if (muro[nx][ny]) return null;

            // Validar portal sin llave: el portal bloquea el paso si no se tiene la llave.
            if (nx == metaX && ny == metaY && !n.llave) return null;

            // Copiar estado mutable para el sucesor
            int m = n.mon;
            int mB = n.mB, cB = n.cB, lB = n.lB;
            boolean l = n.llave;

            // Recoger llave si la hay en la celda destino
            int li = llaveIdx(nx, ny);

            if (li >= 0 && (lB & (1 << li)) != 0 && !l) {
                lB &= ~(1 << li);  // Marcar llave como recogida en bitmask

                l = true;
            }

            // Recoger moneda si la hay (máximo 5 monedas portadas)
            int mi = monIdx(nx, ny);

            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) {
                m++;

                mB &= ~(1 << mi);  // Marcar moneda como recogida
            }

            // Recoger llave duplicada (marca como recogida aunque ya tenga llave).
            // Necesario para que el bitmask refleje correctamente las llaves del mapa.
            int llaveIdx = llaveIdx(nx, ny);

            if (llaveIdx >= 0 && (lB & (1 << llaveIdx)) != 0) {
                lB &= ~(1 << llaveIdx);

                l = true;
            }

            // Comprobar catapulta: si hay una disponible, pagar moneda y activar vuelo
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);

            if (ci != null && (cB & (1 << ci)) != 0) {
                // Catapulta siempre requiere moneda en este agente
                if (m <= 0) return null ;

                m--;  // Pagar 1 moneda

                int[] dir = catDir.get(pk);

                cB &= ~(1 << ci);  // Marcar catapulta como usada

                // Pasar a fase 1 (tick de activación de catapulta)
                return new Nodo(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }

            // Movimiento normal sin catapulta
            return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        // --- FASE 1: Tick de activación de catapulta ---
        } else if (n.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;
            // Transicionar a fase 2 (vuelo) manteniendo dirección y estado
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);

        // --- FASE 2: En vuelo ---
        } else if (n.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = n.x + n.vdx, ty = n.y + n.vdy;

            // Detectar colisión: fuera del mapa, muro sólido (no agua), portal sin llave
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);

            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);

            if (!col && tx == metaX && ty == metaY && !n.llave) col = true;

            if (col) {
                // Aterrizar en la celda actual; si es agua, el agente muere
                if (agua[n.x][n.y]) return null;
                return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 0, 0, 0);
            }

            int nx = tx, ny = ty;
            int m = n.mon;
            int mB = n.mB, cB = n.cB, lB = n.lB;
            boolean l = n.llave;

            // Recoger moneda en vuelo
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }

            // Recoger llave en vuelo
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0 && !l) { lB &= ~(1 << li); l = true; }

            // Victoria en vuelo: aterrizar en el portal con llave → fase 0
            if (nx == metaX && ny == metaY && l) {
                return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            }

            // Encadenamiento de catapultas durante el vuelo → fase 3
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);

            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);

                cB &= ~(1 << ci);

                return new Nodo(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }

            // Seguir volando en la misma dirección
            return new Nodo(nx, ny, m, l, mB, lB, cB, 2, n.vdx, n.vdy);

        // --- FASE 3: Rebote en catapulta encadenada ---
        } else if (n.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            // Transicionar a fase 2 con la nueva dirección de la catapulta
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);
        }
        return null;
    }


    // =====================================================================
    //  UTILIDADES DE CONVERSIÓN DE COORDENADAS
    // =====================================================================

    /**
     * Convierte coordenada X de píxeles a columna de la cuadrícula.
     * @param p Vector de posición en píxeles.
     * @return Índice de columna.
     */
    private int gx(Vector2d p) { return (int)(p.x / blockSize); }

    /**
     * Convierte coordenada Y de píxeles a fila de la cuadrícula.
     * @param p Vector de posición en píxeles.
     * @return Índice de fila.
     */
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }

    /**
     * Codifica una posición (x, y) en un único long para usar como clave
     * en HashMaps. La codificación y * gridW + x es inyectiva para
     * coordenadas válidas. Se usa long en vez de crear objetos Point o
     * String para evitar overhead de GC durante la búsqueda.
     *
     * @param x Columna en la cuadrícula.
     * @param y Fila en la cuadrícula.
     * @return Valor long único que representa la posición.
     */
    private long enc(int x, int y) { return (long)y * gridW + x; }

    /**
     * Devuelve el desplazamiento {dx, dy} correspondiente a una acción.
     * El eje Y crece hacia abajo (convenio GVGAI):
     * RIGHT=(+1,0), LEFT=(-1,0), UP=(0,-1), DOWN=(0,+1).
     *
     * @param a Acción del agente.
     * @return Array {dx, dy} con el desplazamiento.
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
     * Busca el índice de la moneda en la posición (x, y) para el bitmask mB.
     * @param x Columna de la celda.
     * @param y Fila de la celda.
     * @return Índice 0..numMon-1, o -1 si no hay moneda.
     */
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numMon; i++) if (monPos[i] == k) return i;
        return -1;
    }

    /**
     * Busca el índice de la llave en la posición (x, y) para el bitmask lB.
     * @param x Columna de la celda.
     * @param y Fila de la celda.
     * @return Índice 0..numLlaves-1, o -1 si no hay llave.
     */
    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }


    // =====================================================================
    //  CLASE INTERNA — Nodo (estado del juego para DFS)
    // =====================================================================

    /**
     * Representa un estado completo del juego para la búsqueda DFS.
     *
     * A diferencia de AgenteAStar (que separa Estado y Nodo con punteros
     * de padre), aquí el Nodo solo contiene el estado del juego. Los
     * punteros de padre y las acciones se almacenan en mapas externos
     * (padreKey, padreAccion), lo que permite la reconstrucción del plan
     * sin encadenar objetos Nodo directamente.
     *
     * CAMPOS:
     * - x, y:     Posición del agente en la cuadrícula (celdas).
     * - mon:      Número de monedas que porta el agente (0-5).
     * - llave:    true si el agente posee la llave.
     * - mB:       Bitmask de monedas disponibles (1=disponible, 0=recogida).
     * - lB:       Bitmask de llaves disponibles (1=disponible, 0=recogida).
     * - cB:       Bitmask de catapultas disponibles (1=sin usar, 0=usada).
     * - fase:     Fase de vuelo (0=normal, 1=catapulta, 2=vuelo, 3=rebote).
     * - vdx, vdy: Dirección de vuelo actual (solo relevante en fases 1-3).
     *
     * Los bitmasks se inicializan a (1<<n)-1 (todos los bits a 1 = todos
     * los recursos disponibles). Al recoger/usar un recurso, su bit se
     * pone a 0 con la operación AND NOT: bitmask &= ~(1 << idx).
     */
    private static class Nodo {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        /**
         * Constructor del nodo.
         * @param x  Columna del agente.
         * @param y  Fila del agente.
         * @param m  Monedas portadas.
         * @param l  Posesión de llave.
         * @param mB Bitmask de monedas disponibles.
         * @param lB Bitmask de llaves disponibles.
         * @param cB Bitmask de catapultas disponibles.
         * @param f  Fase de vuelo (0-3).
         * @param vx Componente X de dirección de vuelo.
         * @param vy Componente Y de dirección de vuelo.
         */
        Nodo(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB; fase = f; vdx = vx; vdy = vy;
        }

        /**
         * Genera una clave única (String) que identifica completamente este estado.
         * Incluye todos los campos relevantes para evitar colisiones entre estados
         * distintos que comparten posición pero difieren en recursos o fase de vuelo.
         * Se usa como clave en visitados, padreKey y padreAccion.
         *
         * @return Clave del estado como String "x,y,mon,llave,mB,lB,cB,fase,vdx,vdy".
         */
        String key() {
            return x + "," + y + "," + mon + "," + (llave ? 1 : 0) + ","
                 + mB + "," + lB + "," + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }
}