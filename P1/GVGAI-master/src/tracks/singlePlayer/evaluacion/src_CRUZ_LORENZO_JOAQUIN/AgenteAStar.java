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
 * AGENTE A* — Búsqueda offline óptima con heurística admisible
 * ============================================================================
 *
 * Implementación del algoritmo A* (Hart, Nilsson y Raphael, 1968) para el
 * entorno de juego basado en catapultas del framework GVGAI, correspondiente
 * a la Práctica 1 de la asignatura Técnicas de los Sistemas Inteligentes
 * (TSI), curso 2025-26, Universidad de Granada.
 *
 * -------------------------------------------------------------------------
 * QUÉ HACE ESTE AGENTE
 * -------------------------------------------------------------------------
 * Planifica de forma offline (antes de ejecutar ningún movimiento) la ruta
 * óptima desde la posición inicial del avatar hasta el portal de salida.
 * Para ello, explora el espacio de estados utilizando A*, que combina el
 * coste acumulado g(n) con una estimación heurística h(n) para guiar la
 * búsqueda hacia la solución de menor coste total f(n) = g(n) + h(n).
 *
 * -------------------------------------------------------------------------
 * CÓMO LO HACE
 * -------------------------------------------------------------------------
 * 1. En el constructor se parsea el mapa completo una sola vez, extrayendo
 *    muros, agua, catapultas (con dirección hardcodeada por itype),
 *    monedas, llaves y la posición del portal.
 *
 * 2. En la primera llamada a act(), se ejecuta A* sobre un modelo interno
 *    del estado (clase Estado) que codifica posición, monedas recogidas,
 *    posesión de llave, catapultas usadas y fase de vuelo.
 *
 * 3. Las llamadas posteriores a act() simplemente devuelven la siguiente
 *    acción del plan precalculado.
 *
 * -------------------------------------------------------------------------
 * POR QUÉ SE DISEÑA ASÍ
 * -------------------------------------------------------------------------
 * - A* es una búsqueda offline: calcula el plan completo antes de actuar.
 *   Esto es posible porque el entorno es determinista y completamente
 *   observable, de modo que no hay incertidumbre que requiera replanificar.
 *
 * - La heurística Manhattan al portal es admisible (nunca sobreestima),
 *   lo que garantiza que A* encuentre la solución óptima en coste.
 *
 * - Las catapultas se detectan por itype hardcodeado (5=DOWN, 6=UP,
 *   7=RIGHT, 8=LEFT) en lugar de usar simulación con advance(), porque
 *   la simulación en el constructor es lenta e inestable.
 *
 * - El estado incluye bitmasks para monedas (mB), llaves (lB) y
 *   catapultas (cB), lo que permite representar eficientemente qué
 *   recursos quedan disponibles sin duplicar estructuras de datos.
 *
 * - El sistema de 4 fases (0=normal, 1=en catapulta, 2=en vuelo,
 *   3=aterrizaje en otra catapulta) modela fielmente la mecánica de
 *   vuelo del juego, donde cada fase consume un tick de juego.
 *
 * -------------------------------------------------------------------------
 * DESEMPATE EN LA COLA DE PRIORIDAD
 * -------------------------------------------------------------------------
 * Cuando dos nodos tienen el mismo f(n):
 *   1º) Se prefiere el de menor h(n) — más cerca de la meta.
 *   2º) Se prefiere el insertado primero (FIFO) — orden de inserción.
 *
 * -------------------------------------------------------------------------
 * ORDEN DE EXPANSIÓN DE ACCIONES
 * -------------------------------------------------------------------------
 * En fase 0 (caminar): RIGHT, UP, LEFT, DOWN (según pseudocódigo del curso).
 * En fases 1-3 (catapulta/vuelo): solo ACTION_NIL (el agente no controla).
 *
 * @author Joaquín Cruz Lorenzo
 */
public class AgenteAStar extends AbstractPlayer {

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
     * Matrices booleanas que indican si una celda es un muro o agua.
     * Se usa muro[][] para bloquear el paso al caminar; agua[][] distingue
     * agua de muro sólido porque la mecánica de colisión en vuelo difiere:
     * un muro sólido detiene el vuelo pero el agente aterriza antes del muro,
     * mientras que caer sobre agua mata al agente.
     */
    private boolean[][] muro, agua;

    /**
     * Mapa de catapultas: asocia la posición codificada (long) de cada
     * catapulta con su vector de lanzamiento (dx, dy).
     * Se usa HashMap porque el número de catapultas es pequeño y el acceso
     * por clave es O(1).
     */
    private HashMap<Long, int[]> catDir;

    /**
     * Índice de cada catapulta para el bitmask del estado.
     * Cada catapulta recibe un índice 0..numCats-1 que se usa como posición
     * de bit en el campo cB del Estado, permitiendo rastrear qué catapultas
     * se han usado.
     */
    private HashMap<Long, Integer> catIdx;

    /** Número total de catapultas en el mapa. */
    private int numCats;

    /** Array con las posiciones codificadas de cada moneda del mapa. */
    private long[] monPos;

    /** Número total de monedas en el mapa. */
    private int numMon;

    /** Array con las posiciones codificadas de cada llave del mapa. */
    private long[] llavePos;

    /** Número total de llaves en el mapa. */
    private int numLlaves;

    /**
     * Flag que indica si las catapultas son gratuitas (no cuestan moneda).
     * Esto ocurre cuando el mapa no contiene monedas (numMon == 0), en cuyo
     * caso la mecánica del juego permite usar catapultas sin pagar.
     */
    private boolean catapultasGratis;

    // =====================================================================
    //  ATRIBUTOS DE LA BÚSQUEDA
    // =====================================================================

    /** Plan de acciones calculado por A* (null hasta la primera llamada a act). */
    private ArrayList<ACTIONS> plan = null;

    /** Contador de nodos expandidos (sacados de abiertos y procesados). */
    private int nodosExp = 0;

    /** Profundidad máxima alcanzada durante la búsqueda. */
    private int profMax = 0;

    /** Nodos que quedan en la lista de abiertos al terminar. */
    private int nodosAbiertos = 0;

    /** Nodos que se han movido a cerrados al terminar. */
    private int nodosCerrados = 0;

    /**
     * Orden fijo de expansión de acciones en fase 0 (caminando).
     * Se usa RIGHT, UP, LEFT, DOWN siguiendo el convenio establecido
     * en el pseudocódigo de las diapositivas del curso, lo que asegura
     * reproducibilidad de resultados frente al solucionario.
     */
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN
    };


    // =====================================================================
    //  DETECCIÓN DE CATAPULTAS POR ITYPE
    // =====================================================================

    /**
     * Devuelve el vector de lanzamiento (dx, dy) según el itype de la catapulta.
     *
     * QUÉ: Traduce el identificador numérico (itype) de una catapulta en GVGAI
     *       a su dirección de lanzamiento como vector unitario en la cuadrícula.
     *
     * POR QUÉ HARDCODEADO: Los itypes de las catapultas son constantes definidas
     * en el fichero de descripción del juego. Detectar la dirección mediante
     * simulación (advance()) es lento, inestable dentro del constructor y produjo
     * bugs en agentes anteriores. La correspondencia itype→dirección se verificó
     * empíricamente y es estable para todos los mapas de la práctica.
     *
     * @param itype Tipo del objeto inmóvil según GVGAI.
     * @return Vector {dx, dy} de lanzamiento, o null si el itype no es catapulta.
     */
    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0, 1};   // Catapulta DOWN  → lanza hacia abajo
            case 6: return new int[]{0, -1};  // Catapulta UP    → lanza hacia arriba
            case 7: return new int[]{1, 0};   // Catapulta RIGHT → lanza hacia la derecha
            case 8: return new int[]{-1, 0};  // Catapulta LEFT  → lanza hacia la izquierda
            default: return null;             // No es catapulta
        }
    }


    // =====================================================================
    //  CONSTRUCTOR — Parseo completo del mapa
    // =====================================================================

    /**
     * Constructor del agente A*.
     *
     * QUÉ HACE: Lee toda la información estática del mapa una sola vez a partir
     * del StateObservation inicial: dimensiones, posición del avatar, portal,
     * muros, agua, catapultas (con dirección e índice bitmask), monedas y llaves.
     *
     * CÓMO LO HACE:
     * 1. Extrae las dimensiones de la cuadrícula y la posición del avatar.
     * 2. Localiza el portal de salida (meta del agente).
     * 3. Recorre todos los objetos inmóviles clasificándolos por itype:
     *    - itype 0  → muro (bloquea paso)
     *    - itype 3  → agua (bloquea paso y mata si el agente cae en vuelo)
     *    - itype 5-8 → catapultas (se registran con dirección e índice bitmask)
     * 4. Recorre los recursos clasificándolos por itype:
     *    - itype 15 → moneda (recurso necesario para usar catapultas)
     *    - itype 16 → llave (necesaria para abrir el portal), excluyendo la
     *                  posición inicial del avatar para evitar contarla dos veces
     *                  si el avatar empieza sobre una llave.
     * 5. Determina si las catapultas son gratuitas (mapa sin monedas).
     *
     * POR QUÉ EN EL CONSTRUCTOR: Toda esta información es estática (el mapa no
     * cambia durante la partida), así que se parsea una sola vez para evitar
     * repetir trabajo en cada tick del juego.
     *
     * @param so    Estado inicial del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU (no se usa aquí, pero lo requiere la interfaz).
     */
    public AgenteAStar(StateObservation so, ElapsedCpuTimer timer) {
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

        // --- Posición inicial del avatar (convertida de píxeles a celdas) ---
        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // --- Localizar el portal de salida (meta) ---
        ArrayList<Observation>[] portales = so.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gx(portales[0].get(0).position);
            metaY = gy(portales[0].get(0).position);
        }

        // --- Clasificar objetos inmóviles: muros, agua y catapultas ---
        ArrayList<long[]> catList = new ArrayList<>();
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null) {
            for (ArrayList<Observation> lista : inmov) {
                for (Observation obs : lista) {
                    int x = gx(obs.position), y = gy(obs.position);
                    // Ignorar objetos fuera del mapa (seguridad)
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;

                    if (obs.itype == 0) {
                        // Muro sólido: bloquea el paso al caminar y detiene el vuelo
                        muro[x][y] = true;
                    } else if (obs.itype == 3) {
                        // Agua: bloquea el paso al caminar (se marca también como muro)
                        // y mata al agente si aterriza en ella durante el vuelo.
                        // Se distingue de muro sólido con agua[][] para la lógica de vuelo.
                        agua[x][y] = true;
                        muro[x][y] = true;
                    } else {
                        // Intentar interpretar como catapulta por su itype
                        int[] dir = catapultDir(obs.itype);
                        if (dir != null) {
                            long pk = enc(x, y);
                            catDir.put(pk, dir);      // Registrar dirección de lanzamiento
                            catList.add(new long[]{pk}); // Añadir a la lista para indexar
                        }
                    }
                }
            }
        }

        // Asignar un índice secuencial (0, 1, 2...) a cada catapulta para el bitmask.
        // Este índice determina qué bit del campo cB del Estado controla cada catapulta.
        int ci = 0;
        for (long[] cl : catList) catIdx.put(cl[0], ci++);
        numCats = ci;

        // --- Clasificar recursos: monedas y llaves ---
        ArrayList<Long> ml = new ArrayList<>();   // Posiciones de monedas
        ArrayList<Long> kl = new ArrayList<>();   // Posiciones de llaves
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) {
                        // Moneda: recurso consumible al usar catapultas
                        ml.add(enc(gx(obs.position), gy(obs.position)));
                    } else if (obs.itype == 16) {
                        // Llave: necesaria para abrir el portal de salida
                        int kx = gx(obs.position), ky = gy(obs.position);
                        // Excluir la posición del avatar: si el avatar empieza
                        // sobre una llave, GVGAI la recoge automáticamente y no
                        // debe contarse como recurso pendiente del mapa.
                        if (kx != iniX || ky != iniY) {
                            kl.add(enc(kx, ky));
                        }
                    }
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

        // Si no hay monedas, las catapultas se pueden usar sin coste
        catapultasGratis = (numMon == 0);
    }


    // =====================================================================
    //  ACT — Punto de entrada del framework GVGAI en cada tick
    // =====================================================================

    /**
     * Método llamado por GVGAI en cada tick del juego para obtener la acción
     * que el agente desea ejecutar.
     *
     * QUÉ HACE:
     * - En la primera llamada: ejecuta A* completo, almacena el plan resultante
     *   y registra las métricas de la búsqueda (nodos expandidos, abiertos,
     *   cerrados, longitud del plan y tiempo).
     * - En llamadas posteriores: extrae y devuelve la siguiente acción del plan.
     * - Si el plan se agota: devuelve ACTION_NIL (no hacer nada).
     *
     * POR QUÉ SOLO SE BUSCA UNA VEZ: A* es una búsqueda offline que calcula
     * el camino completo. Al ser el entorno determinista y totalmente observable,
     * no es necesario replanificar: el plan precalculado sigue siendo válido
     * durante toda la partida.
     *
     * @param so    Estado actual del juego (no se usa tras la primera llamada).
     * @param timer Temporizador de CPU proporcionado por GVGAI.
     * @return La acción a ejecutar en este tick.
     */
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (plan == null) {
            // Primera llamada: ejecutar A* y medir el tiempo
            long t0 = System.currentTimeMillis();
            plan = buscarAStar();

            // Registrar métricas para el solucionario
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setNodosAbiertos(nodosAbiertos);
            mp.setNodosCerrados(nodosCerrados);
            mp.setNumAccionesPlan(plan.size() > 0 ? plan.size() : -1);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("A*");
            mp.printMetrics();
        }

        // Devolver la siguiente acción del plan, o NIL si ya no quedan
        if (!plan.isEmpty()) return plan.remove(0);
        return ACTIONS.ACTION_NIL;
    }


    // =====================================================================
    //  ALGORITMO A* — Búsqueda informada óptima
    // =====================================================================
    //
    //  Pseudocódigo (basado en diapositivas del curso, pág. 26):
    //
    //  abiertos = [nodo_inicial]
    //  cerrados = []
    //  while abiertos no vacío:
    //      actual = extraer nodo con menor f(n) de abiertos
    //      if actual es meta: reconstruir plan y terminar
    //      mover actual a cerrados
    //      for cada sucesor de actual:
    //          if sucesor en cerrados con peor g: reabrirlo en abiertos
    //          elif sucesor no en abiertos ni cerrados: añadir a abiertos
    //          elif sucesor en abiertos con peor g: actualizar en abiertos
    //
    // =====================================================================

    /**
     * Ejecuta el algoritmo A* sobre el espacio de estados del juego.
     *
     * QUÉ HACE: Busca el camino de coste mínimo desde el estado inicial
     * (posición del avatar al empezar) hasta el estado meta (avatar en el
     * portal, con la llave, en fase 0).
     *
     * CÓMO LO HACE:
     * - Mantiene dos estructuras: abiertos (PriorityQueue ordenada por f)
     *   y cerrados (HashMap para consulta O(1) por clave de estado).
     * - En cada iteración, extrae el nodo con menor f de abiertos, comprueba
     *   si es meta, y si no lo es, genera sus sucesores y los gestiona
     *   según estén ya en abiertos, cerrados o sean nuevos.
     *
     * GESTIÓN DE NODOS OBSOLETOS:
     * Java PriorityQueue no soporta decrease-key eficiente. En lugar de
     * eliminar físicamente un nodo cuando se encuentra un camino mejor,
     * se marca el antiguo como obsoleto (obs = true) y se inserta uno nuevo.
     * Al extraer un nodo marcado como obsoleto, simplemente se descarta.
     * Esto es equivalente a lazy deletion y es una técnica estándar para
     * emular decrease-key en colas de prioridad basadas en heaps binarios.
     *
     * @return Lista ordenada de acciones desde el inicio hasta la meta,
     *         o lista vacía si no se encuentra solución.
     */
    private ArrayList<ACTIONS> buscarAStar() {

        // --- Estructuras de abiertos y cerrados ---
        // ab:  cola de prioridad que ordena nodos por f(n), luego h(n), luego FIFO
        // abM: mapa auxiliar para consultar en O(1) si un estado ya está en abiertos
        //      y acceder a su nodo vigente (el de menor g conocido)
        // ce:  mapa de cerrados, indexado por la clave del estado
        PriorityQueue<Nodo> ab = new PriorityQueue<>();
        HashMap<String, Nodo> abM = new HashMap<>();
        HashMap<String, Nodo> ce = new HashMap<>();
        int ord = 0;  // Contador global de inserción para desempate FIFO

        // --- Crear el estado inicial ---
        // Posición del avatar, 0 monedas recogidas, sin llave, todos los bitmasks
        // activos (todas las monedas, llaves y catapultas disponibles), fase 0,
        // sin velocidad de vuelo.
        // Los bitmasks se inicializan a (1<<n)-1, que pone a 1 los n bits menos
        // significativos, representando que todos los recursos están disponibles.
        Estado e0 = new Estado(iniX, iniY, 0, false,
            (1 << numMon) - 1,     // Todas las monedas disponibles
            (1 << numLlaves) - 1,  // Todas las llaves disponibles
            (1 << numCats) - 1,    // Todas las catapultas disponibles
            0, 0, 0);              // Fase 0, sin velocidad de vuelo

        Nodo n0 = new Nodo(e0, null, ACTIONS.ACTION_NIL, 0, heuristica(e0), 0, ord++);
        ab.add(n0);
        abM.put(e0.key(), n0);
        Nodo meta = null;

        // --- Bucle principal de A* ---
        while (!ab.isEmpty()) {
            // Extraer el nodo con menor f(n) de abiertos
            Nodo ac = ab.poll();

            // Descartar nodos obsoletos (reemplazados por uno con mejor g)
            if (ac.obs) continue;
            String ka = ac.e.key();
            if (abM.get(ka) != ac) continue;  // Doble verificación de vigencia
            abM.remove(ka);

            // Actualizar profundidad máxima (para métricas)
            if (ac.pr > profMax) profMax = ac.pr;

            // --- Comprobar si es meta ---
            if (esMeta(ac.e)) {
                meta = ac;
                // Registrar el tamaño de abiertos y cerrados en el momento de encontrar
                // la solución, para reportar métricas consistentes con el solucionario
                this.nodosAbiertos = abM.size();
                this.nodosCerrados = ce.size();
                break;
            }

            // --- Expandir el nodo actual ---
            nodosExp++;
            nodosCerrados++;
            ce.put(ka, ac);  // Mover a cerrados

            // Determinar qué acciones son aplicables según la fase del estado:
            // - Fase 0 (caminando): las 4 direcciones en el orden establecido
            // - Fases 1-3 (catapulta/vuelo): solo NIL (el agente no tiene control)
            ACTIONS[] acciones;
            if (ac.e.fase == 0) {
                acciones = ORDEN;  // RIGHT, UP, LEFT, DOWN
            } else {
                acciones = new ACTIONS[]{ACTIONS.ACTION_NIL};
            }

            // Generar y procesar cada sucesor
            for (ACTIONS a : acciones) {
                Estado h = trans(ac.e, a);  // Función de transición
                if (h == null) continue;    // Movimiento inválido (muro, fuera de mapa, etc.)

                int gN = ac.g + 1;          // Coste uniforme: cada acción cuesta 1
                double hN = heuristica(h);  // Estimación al portal
                String kS = h.key();        // Clave única del estado sucesor

                // Gestionar la inserción/actualización en abiertos/cerrados
                añadirSucesor(h, kS, gN, hN, ac, a, ab, abM, ce, ord++);
            }
        }

        // --- Reconstruir el plan de acciones ---
        // Se recorre la cadena de padres desde la meta hasta el nodo inicial,
        // apilando las acciones y luego vaciando la pila para obtener el orden correcto.
        ArrayList<ACTIONS> r = new ArrayList<>();
        if (meta != null) {
            Deque<ACTIONS> p = new ArrayDeque<>();
            for (Nodo n = meta; n.padre != null; n = n.padre) p.push(n.accion);
            while (!p.isEmpty()) r.add(p.pop());
        }
        return r;
    }


    // =====================================================================
    //  HEURÍSTICA — Distancia Manhattan al portal
    // =====================================================================

    /**
     * Calcula la heurística h(n) para un estado dado.
     *
     * QUÉ: Devuelve la distancia Manhattan entre la posición del agente y
     *       el portal de salida.
     *
     * POR QUÉ ES ADMISIBLE: En una cuadrícula donde cada movimiento desplaza
     * exactamente una celda en una dirección cardinal, la distancia Manhattan
     * es el mínimo número de pasos necesarios para llegar al destino sin
     * considerar obstáculos. Por tanto, nunca sobreestima el coste real,
     * lo que garantiza la optimalidad de A*.
     *
     * POR QUÉ NO CONSIDERA LA LLAVE: Incluir la distancia a la llave
     * complicaría la heurística y podría hacerla no admisible en ciertos
     * escenarios con catapultas. La Manhattan pura al portal es simple,
     * admisible y produce resultados consistentes con el solucionario.
     *
     * @param e Estado del que calcular la heurística.
     * @return Valor h(n) ≥ 0 (distancia Manhattan al portal).
     */
    private double heuristica(Estado e) {
        return Math.abs(e.x - metaX) + Math.abs(e.y - metaY);
    }


    // =====================================================================
    //  CONDICIÓN DE META
    // =====================================================================

    /**
     * Comprueba si un estado es meta (fin del juego con victoria).
     *
     * QUÉ: El agente gana cuando está en el portal, tiene la llave y se
     *       encuentra en fase 0 (caminando normalmente, no en vuelo).
     *
     * POR QUÉ SE REQUIERE FASE 0: Si el agente llega al portal en pleno
     * vuelo de catapulta (fases 1-3), el juego no lo reconoce como victoria
     * porque el agente aún está en transición. Solo al aterrizar (fase 0)
     * se activa la interacción con el portal.
     *
     * @param e Estado a evaluar.
     * @return true si el estado es meta, false en caso contrario.
     */
    private boolean esMeta(Estado e) {
        return e.x == metaX && e.y == metaY && e.llave && e.fase == 0;
    }


    // =====================================================================
    //  FUNCIÓN DE TRANSICIÓN — Motor de física del juego
    // =====================================================================

    /**
     * Calcula el estado resultante de aplicar una acción a un estado dado.
     *
     * QUÉ HACE: Modela fielmente la mecánica del juego, incluyendo movimiento
     * normal, recogida de monedas/llaves, uso de catapultas con coste de
     * monedas, y el sistema completo de vuelo en 4 fases.
     *
     * CÓMO FUNCIONA SEGÚN LA FASE:
     *
     * FASE 0 — CAMINANDO (el agente tiene control):
     *   - Solo acepta acciones direccionales (no NIL).
     *   - Calcula la nueva posición y verifica límites del mapa y muros.
     *   - Impide entrar al portal sin llave (el portal actúa como muro).
     *   - Recoge monedas (si hay disponibles y no se tiene el máximo de 5).
     *   - Recoge llaves (marca como poseída si no la tenía).
     *   - Si la nueva celda tiene catapulta disponible:
     *       · Paga 1 moneda (si no son gratis) o rechaza si no tiene monedas.
     *       · Consume la catapulta (bit a 0 en cB) y pasa a fase 1.
     *   - Si no hay catapulta, permanece en fase 0.
     *
     * FASE 1 — SOBRE LA CATAPULTA (tick de preparación):
     *   - Solo acepta NIL (el agente no controla).
     *   - Transiciona a fase 2 (inicio del vuelo) sin mover al agente.
     *   - Este tick intermedio replica el comportamiento de GVGAI donde
     *     la catapulta necesita un tick para "activarse".
     *
     * FASE 2 — EN VUELO:
     *   - Solo acepta NIL (el agente no controla).
     *   - Avanza una celda en la dirección de vuelo (vdx, vdy).
     *   - Detecta colisión con: bordes del mapa, muros sólidos, o portal sin llave.
     *     · Si colisiona: aterriza en la celda anterior (fase 0).
     *       Excepción: si esa celda es agua, el agente muere (retorna null).
     *     · Si no colisiona: puede recoger monedas/llaves en la celda de destino.
     *       - Si aterriza en el portal con llave: fase 0 (victoria).
     *       - Si aterriza en otra catapulta disponible: fase 3 (encadenamiento).
     *       - En otro caso: sigue en fase 2 con la misma dirección.
     *
     * FASE 3 — ATERRIZAJE EN CATAPULTA DURANTE VUELO:
     *   - Solo acepta NIL.
     *   - Transiciona a fase 2 con la nueva dirección de la catapulta.
     *   - Permite encadenar catapultas (el agente rebota de una a otra).
     *
     * POR QUÉ SE MODELA ASÍ: Cada fase corresponde exactamente a un tick del
     * juego en GVGAI. Modelar las fases garantiza que el plan generado tenga
     * la longitud correcta y las acciones se alineen con los ticks reales.
     *
     * @param e Estado actual.
     * @param a Acción a aplicar.
     * @return Nuevo estado resultante, o null si la acción es inválida/mortal.
     */
    private Estado trans(Estado e, ACTIONS a) {

        // ---------------------------------------------------------------
        //  FASE 0: Caminando — el agente elige dirección
        // ---------------------------------------------------------------
        if (e.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;  // En fase 0 se debe mover

            int[] d = delta(a);  // Obtener desplazamiento (dx, dy) de la acción
            int nx = e.x + d[0], ny = e.y + d[1];

            // Validar límites del mapa
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            // Validar que no sea muro
            if (muro[nx][ny]) return null;
            // No se puede entrar al portal sin llave (actúa como muro)
            if (nx == metaX && ny == metaY && !e.llave) return null;

            // Copiar el estado mutable para el sucesor
            int m = e.mon; boolean l = e.llave;
            int mB = e.mB, lB = e.lB, cB = e.cB;

            // Intentar recoger moneda en la nueva posición
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) {
                // Hay moneda disponible (bit activo) y no se ha alcanzado el máximo (5)
                m++;
                mB &= ~(1 << mi);  // Marcar moneda como recogida (bit a 0)
            }

            // Intentar recoger llave en la nueva posición
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);    // Marcar llave como recogida en el bitmask
                if (!l) l = true;     // Activar posesión de llave si no la tenía
            }

            // Comprobar si la nueva celda tiene una catapulta disponible
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                // Hay catapulta y aún no se ha usado (bit activo en cB)
                if (!catapultasGratis && m <= 0) return null;  // Sin monedas: no puede usar
                if (!catapultasGratis) m--;  // Pagar 1 moneda
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);  // Marcar catapulta como usada
                // Pasar a fase 1 (preparación de vuelo) con la dirección de la catapulta
                return new Estado(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }

            // Movimiento normal sin catapulta: permanece en fase 0
            return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        // ---------------------------------------------------------------
        //  FASE 1: Sobre la catapulta — tick de activación
        // ---------------------------------------------------------------
        } else if (e.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;  // Solo NIL en fases de vuelo
            // Transicionar a fase 2 (vuelo) manteniendo dirección y estado
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB,
                              2, e.vdx, e.vdy);

        // ---------------------------------------------------------------
        //  FASE 2: En vuelo — avance automático en la dirección de lanzamiento
        // ---------------------------------------------------------------
        } else if (e.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;

            // Calcular posición objetivo (siguiente celda en la dirección de vuelo)
            int tx = e.x + e.vdx, ty = e.y + e.vdy;

            // Detectar colisión: bordes del mapa, muro sólido (no agua), portal sin llave
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);  // Muro sólido detiene, agua no
            if (!col && tx == metaX && ty == metaY && !e.llave) col = true;  // Portal sin llave

            if (col) {
                // Colisión: el agente aterriza en la celda actual (no avanza)
                // Si la celda actual es agua, el agente muere → estado inválido
                if (agua[e.x][e.y]) return null;
                // Aterrizar en fase 0
                return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB, 0, 0, 0);
            }

            // Sin colisión: avanzar a la celda objetivo
            int nx = tx, ny = ty;
            int m = e.mon; boolean l = e.llave;
            int mB = e.mB, lB = e.lB, cB = e.cB;

            // Recoger moneda en vuelo (si la hay)
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) {
                m++;
                mB &= ~(1 << mi);
            }

            // Recoger llave en vuelo (si la hay)
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);
                if (!l) l = true;
            }

            // Si aterriza en el portal con llave: victoria (fase 0)
            if (nx == metaX && ny == metaY && l) {
                return new Estado(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            }

            // Si aterriza en otra catapulta disponible: encadenamiento (fase 3)
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);  // Consumir la catapulta encadenada
                return new Estado(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }

            // Seguir volando en la misma dirección
            return new Estado(nx, ny, m, l, mB, lB, cB, 2, e.vdx, e.vdy);

        // ---------------------------------------------------------------
        //  FASE 3: Aterrizaje en catapulta durante vuelo — rebote
        // ---------------------------------------------------------------
        } else if (e.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            // Transicionar a fase 2 con la nueva dirección de la catapulta encadenada
            return new Estado(e.x, e.y, e.mon, e.llave, e.mB, e.lB, e.cB,
                              2, e.vdx, e.vdy);
        }

        return null;  // Fase desconocida (no debería ocurrir)
    }


    // =====================================================================
    //  UTILIDADES DE CONVERSIÓN DE COORDENADAS
    // =====================================================================

    /**
     * Convierte una coordenada X de píxeles a celdas de la cuadrícula.
     * @param p Vector de posición en píxeles.
     * @return Índice de columna en la cuadrícula.
     */
    private int gx(Vector2d p) { return (int)(p.x / blockSize); }

    /**
     * Convierte una coordenada Y de píxeles a celdas de la cuadrícula.
     * @param p Vector de posición en píxeles.
     * @return Índice de fila en la cuadrícula.
     */
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }

    /**
     * Codifica una posición (x, y) en un único valor long.
     * Se usa como clave en los HashMaps de catapultas para búsqueda O(1).
     * La codificación y * gridW + x es inyectiva para coordenadas válidas.
     *
     * @param x Columna en la cuadrícula.
     * @param y Fila en la cuadrícula.
     * @return Valor long único que representa la posición.
     */
    private long enc(int x, int y) { return (long)y * gridW + x; }

    /**
     * Devuelve el desplazamiento (dx, dy) correspondiente a una acción.
     * El sistema de coordenadas sigue el convenio de GVGAI:
     *   - RIGHT → (+1, 0), LEFT → (-1, 0)
     *   - UP → (0, -1), DOWN → (0, +1)
     * (El eje Y crece hacia abajo, como es habitual en gráficos 2D).
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
     * Busca el índice de la moneda en la posición (x, y), si existe.
     * Se usa para consultar/modificar el bit correspondiente en el bitmask mB.
     *
     * @param x Columna de la celda.
     * @param y Fila de la celda.
     * @return Índice de la moneda (0..numMon-1), o -1 si no hay moneda ahí.
     */
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numMon; i++) if (monPos[i] == k) return i;
        return -1;
    }

    /**
     * Busca el índice de la llave en la posición (x, y), si existe.
     * Se usa para consultar/modificar el bit correspondiente en el bitmask lB.
     *
     * @param x Columna de la celda.
     * @param y Fila de la celda.
     * @return Índice de la llave (0..numLlaves-1), o -1 si no hay llave ahí.
     */
    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }


    // =====================================================================
    //  GESTIÓN DE SUCESORES EN A*
    // =====================================================================

    /**
     * Gestiona la inserción o actualización de un nodo sucesor en las
     * estructuras de abiertos y cerrados, implementando la lógica de
     * reapertura y actualización del pseudocódigo de A*.
     *
     * QUÉ HACE: Dado un sucesor con coste g tentativo, decide:
     *   1. Si está en CERRADOS con g peor → reabrirlo (moverlo a abiertos).
     *   2. Si no está en cerrados ni en abiertos → insertarlo en abiertos.
     *   3. Si está en ABIERTOS con g peor → reemplazarlo (lazy deletion).
     *
     * CÓMO FUNCIONA EL REEMPLAZO (LAZY DELETION):
     * PriorityQueue de Java no soporta decrease-key. En lugar de eliminar
     * el nodo antiguo (operación O(n)), se marca como obsoleto (obs = true)
     * y se inserta uno nuevo con el mejor g. Cuando el nodo obsoleto salga
     * de la cola en el bucle principal, se descartará sin procesarlo.
     *
     * @param h   Estado sucesor.
     * @param kS  Clave del estado sucesor (para indexar en los mapas).
     * @param gN  Coste g del camino al sucesor a través del nodo actual.
     * @param hN  Valor heurístico h(n) del sucesor.
     * @param ac  Nodo padre (el que se está expandiendo).
     * @param a   Acción que lleva del padre al sucesor.
     * @param ab  Cola de prioridad de abiertos.
     * @param abM Mapa auxiliar de abiertos (clave → nodo vigente).
     * @param ce  Mapa de cerrados (clave → nodo).
     * @param ord Orden de inserción global (para desempate FIFO).
     */
    private void añadirSucesor(Estado h, String kS, int gN, double hN,
        Nodo ac, ACTIONS a,
        PriorityQueue<Nodo> ab, HashMap<String,Nodo> abM,
        HashMap<String,Nodo> ce, int ord) {

        // Caso 1: El sucesor está en cerrados
        Nodo eC = ce.get(kS);
        if (eC != null) {
            if (gN < eC.g) {
                // Se ha encontrado un camino más corto al sucesor que ya estaba
                // en cerrados → reabrirlo (quitarlo de cerrados, ponerlo en abiertos)
                ce.remove(kS);
                Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord);
                ab.add(ns);
                abM.put(kS, ns);
            }
            // Si gN >= eC.g, el camino actual no es mejor → no hacer nada
            return;
        }

        // Caso 2 y 3: El sucesor NO está en cerrados
        Nodo eA = abM.get(kS);
        if (eA == null) {
            // Caso 2: No está en abiertos → insertarlo por primera vez
            Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord);
            ab.add(ns);
            abM.put(kS, ns);
        } else if (gN < eA.g) {
            // Caso 3: Ya está en abiertos pero con peor g → reemplazar
            // Marcar el antiguo como obsoleto y añadir el nuevo
            eA.obs = true;
            Nodo ns = new Nodo(h, ac, a, gN, hN, ac.pr + 1, ord);
            ab.add(ns);
            abM.put(kS, ns);
        }
        // Si gN >= eA.g, el camino actual no mejora → no hacer nada
    }


    // =====================================================================
    //  CLASES INTERNAS — Estado y Nodo
    // =====================================================================

    /**
     * Representa un estado completo del juego para la búsqueda A*.
     *
     * QUÉ CODIFICA: Toda la información necesaria para distinguir dos
     * situaciones de juego diferentes y determinar las transiciones válidas.
     *
     * POR QUÉ INCLUYE BITMASKS:
     * En lugar de usar listas o conjuntos para rastrear qué monedas, llaves
     * y catapultas quedan disponibles, se usan enteros como bitmasks donde
     * cada bit corresponde a un recurso. Esto es eficiente en memoria y
     * permite generar claves de estado compactas para los HashMaps.
     * - Bit 1 = recurso disponible, Bit 0 = recurso ya recogido/usado.
     * - Inicialización: (1 << n) - 1 pone los n bits menos significativos a 1.
     *
     * CAMPOS:
     * - x, y:   Posición del agente en la cuadrícula (celdas).
     * - mon:    Número de monedas que lleva el agente (0-5).
     * - llave:  true si el agente posee la llave.
     * - mB:     Bitmask de monedas disponibles en el mapa.
     * - lB:     Bitmask de llaves disponibles en el mapa.
     * - cB:     Bitmask de catapultas disponibles en el mapa.
     * - fase:   Fase de vuelo actual (0=normal, 1=catapulta, 2=vuelo, 3=rebote).
     * - vdx, vdy: Dirección de vuelo actual (solo relevante en fases 1-3).
     */
    private static class Estado {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        /**
         * Constructor del estado.
         * @param x  Coordenada X (columna) del agente.
         * @param y  Coordenada Y (fila) del agente.
         * @param m  Número de monedas que porta el agente.
         * @param l  Posesión de la llave.
         * @param mB Bitmask de monedas disponibles.
         * @param lB Bitmask de llaves disponibles.
         * @param cB Bitmask de catapultas disponibles.
         * @param f  Fase de vuelo (0-3).
         * @param vx Componente X de la dirección de vuelo.
         * @param vy Componente Y de la dirección de vuelo.
         */
        Estado(int x, int y, int m, boolean l, int mB, int lB, int cB,
               int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB;
            fase = f; vdx = vx; vdy = vy;
        }

        /**
         * Genera una clave única (String) que identifica completamente este estado.
         *
         * QUÉ: Serializa todos los campos del estado en un String.
         *
         * POR QUÉ: Se usa como clave en los HashMaps de abiertos y cerrados.
         * Dos estados con la misma clave son idénticos en todos sus campos,
         * lo que permite detectar duplicados y comparar caminos alternativos
         * al mismo estado.
         *
         * @return Clave del estado como String "x,y,mon,llave,mB,lB,cB,fase,vdx,vdy".
         */
        String key() {
            return x + "," + y + "," + mon + "," + (llave ? 1 : 0) + ","
                + mB + "," + lB + "," + cB + "," + fase + "," + vdx + "," + vdy;
        }
    }

    /**
     * Nodo del árbol de búsqueda A*.
     *
     * QUÉ REPRESENTA: Un estado junto con la información necesaria para la
     * búsqueda: coste acumulado g, heurística h, enlace al padre (para
     * reconstruir el plan), acción que lo generó y metadatos de ordenación.
     *
     * IMPLEMENTA Comparable para funcionar con PriorityQueue de Java.
     * El criterio de comparación determina el orden de extracción:
     *   1º) Menor f(n) = g + h (A* estándar: camino estimado más corto).
     *   2º) Menor h(n) (desempate: preferir nodos más cerca de la meta).
     *   3º) Menor orden de inserción (desempate FIFO: estabilidad).
     */
    private static class Nodo implements Comparable<Nodo> {
        Estado e;         // Estado del juego que representa este nodo
        Nodo padre;       // Nodo predecesor en el camino (null para el inicial)
        ACTIONS accion;   // Acción que se aplicó al padre para llegar aquí
        int g;            // Coste acumulado desde el inicio (cada acción cuesta 1)
        int pr;           // Profundidad en el árbol de búsqueda
        int orden;        // Orden global de inserción (para desempate FIFO)
        double h;         // Valor heurístico h(n)
        boolean obs;      // Flag de obsolescencia (lazy deletion, ver buscarAStar)

        /**
         * Constructor del nodo.
         * @param e Estado asociado.
         * @param p Nodo padre (null para el nodo raíz).
         * @param a Acción que generó este nodo desde el padre.
         * @param g Coste acumulado g(n).
         * @param h Valor heurístico h(n).
         * @param pr Profundidad en el árbol.
         * @param o Orden de inserción (para desempate FIFO).
         */
        Nodo(Estado e, Nodo p, ACTIONS a, int g, double h, int pr, int o) {
            this.e = e; padre = p; accion = a;
            this.g = g; this.h = h; this.pr = pr; orden = o;
        }

        /**
         * Calcula f(n) = g(n) + h(n), la función de evaluación de A*.
         * f(n) estima el coste total del camino óptimo que pasa por este nodo.
         * @return Valor de f(n).
         */
        double f() { return g + h; }

        /**
         * Comparador para la PriorityQueue.
         * Define el orden de extracción de A*:
         *   1. Menor f(n) → se exploran primero los caminos más prometedores.
         *   2. Menor h(n) → ante empate en f, preferir el más cerca de la meta
         *      (esto es equivalente a preferir mayor g, es decir, más avanzado).
         *   3. Menor orden de inserción → estabilidad FIFO para reproducibilidad.
         */
        @Override
        public int compareTo(Nodo o) {
            double f1 = f(), f2 = o.f();
            if (f1 != f2) return Double.compare(f1, f2);
            if (h != o.h) return Double.compare(h, o.h);
            return Integer.compare(orden, o.orden);
        }
    }
}