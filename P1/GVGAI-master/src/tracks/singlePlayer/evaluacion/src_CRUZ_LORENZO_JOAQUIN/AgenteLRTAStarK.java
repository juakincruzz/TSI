package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

/**
 * ============================================================================
 * AGENTE LRTA*(k) — Búsqueda en tiempo real con propagación de heurística
 * ============================================================================
 *
 * Implementación del algoritmo Learning Real-Time A* con parámetro k
 *
 *
 * -------------------------------------------------------------------------
 * QUÉ HACE ESTE AGENTE
 * -------------------------------------------------------------------------
 * Al igual que RTA*, LRTA*(k) es un algoritmo de búsqueda en tiempo real
 * que toma decisiones tick a tick. Sin embargo, incorpora una mejora clave:
 * en cada tick, además de actualizar la heurística del nodo actual, propaga
 * esa actualización a hasta k nodos vecinos cuyo soporte (mejor vecino) sea
 * el nodo que cambió. Esto permite que la información heurística se difunda
 * más rápido por el espacio de estados, mejorando el rendimiento a costa
 * de un mayor tiempo de procesamiento por tick.
 *
 * -------------------------------------------------------------------------
 * CÓMO LO HACE — Pseudocódigo (diapositivas del curso, págs. 32-33)
 * -------------------------------------------------------------------------
 *
 * Bucle principal (pág. 32):
 *   actual = nodo_inicial
 *   forall nodo in Grafo: soporte(nodo) = null
 *   while True:
 *       if actual == objetivo: break
 *       LookaheadUpdateK(actual, k)
 *       mejorVecino = argmin { c(actual,y) + h(y) | y ∈ Sucesores(actual) }
 *       actual = mejorVecino
 *
 * Procedimiento LookaheadUpdateK(actual, k) (pág. 33):
 *   Cola = <actual>
 *   contador = k - 1
 *   while !Cola.empty():
 *       x = Cola.pop()
 *       mejorVecinoX = argmin { c(x,y) + h(y) | y ∈ Sucesores(x) }
 *       soporte(x) = mejorVecinoX
 *       propagar = False
 *       if h(x) < c(x, mejorVecinoX) + h(mejorVecinoX):
 *           propagar = True
 *           h(x) = c(x, mejorVecinoX) + h(mejorVecinoX)    // +1 actualización
 *       if propagar:
 *           foreach sucesor in Sucesores(x):
 *               if contador > 0 and soporte(sucesor) == x:
 *                   Cola.insert(sucesor)
 *                   contador = contador - 1
 *
 * -------------------------------------------------------------------------
 * POR QUÉ SE DISEÑA ASÍ
 * -------------------------------------------------------------------------
 * - LRTA*(k) extiende LRTA* con propagación controlada. LRTA* básico
 *   (equivalente a k=1) solo actualiza el nodo actual en cada tick, lo que
 *   puede ser lento para difundir información heurística por el grafo.
 *   Con k>1, las actualizaciones se propagan a vecinos afectados, acelerando
 *   la convergencia de h(n) hacia h*(n).
 *
 * - La propagación está acotada por el parámetro k (aquí K=5), que limita
 *   el número total de nodos que pueden actualizarse por tick. Esto
 *   garantiza un coste computacional controlado por tick: como máximo se
 *   actualizan k nodos, manteniendo la naturaleza "en tiempo real" del
 *   algoritmo.
 *
 * - La propagación es selectiva: solo se propaga a vecinos cuyo soporte
 *   (mejor vecino) es el nodo que cambió. Esto asegura que solo se
 *   actualizan nodos realmente afectados por el cambio, evitando trabajo
 *   innecesario (criterio de eficiencia de Hernández & Meseguer).
 *
 * - A diferencia de RTA* (que usa el segundo mínimo y puede sobreestimar),
 *   LRTA*(k) usa el PRIMER MÍNIMO en su regla de aprendizaje:
 *       h(x) = max(h(x), min{c(x,y) + h(y)})
 *   Esto mantiene h(n) admisible (nunca sobreestima), lo que es útil si
 *   se requieren múltiples ejecuciones en el mismo mapa: LRTA* converge
 *   más rápido al camino óptimo.
 *
 * - La heurística base es Manhattan al portal (sin llave), consistente
 *   con RTA* y A* de esta práctica.
 *
 * -------------------------------------------------------------------------
 * VALOR DE K
 * -------------------------------------------------------------------------
 * K = 5
 *
 * -------------------------------------------------------------------------
 * CONCEPTO DE SOPORTE
 * -------------------------------------------------------------------------
 * El "soporte" de un nodo x es su mejor vecino: el sucesor y que minimiza
 * c(x,y) + h(y). Se almacena en una tabla aparte (HashMap soporte) y se
 * usa para decidir si propagar una actualización: cuando h(x) cambia, solo
 * tiene sentido propagar a vecinos z tales que soporte(z) == x, porque
 * son los únicos cuyo valor heurístico podría verse afectado (su mejor
 * camino pasaba por x, y x acaba de cambiar).
 *
 * -------------------------------------------------------------------------
 * CATAPULTAS Y FASES DE VUELO
 * -------------------------------------------------------------------------
 * Detección hardcodeada: 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT.
 * Sistema de 4 fases (0=normal, 1=catapulta, 2=vuelo, 3=rebote) idéntico
 * a AgenteAStar y AgenteRTAStar.
 *
 * -------------------------------------------------------------------------
 * ORDEN DE EXPANSIÓN
 * -------------------------------------------------------------------------
 * RIGHT, UP, LEFT, DOWN — desempate implícito por orden de iteración,
 * consistente con los demás agentes y el pseudocódigo del curso.
 *
 * @author Joaquín Cruz Lorenzo
 */
public class AgenteLRTAStarK extends AbstractPlayer {

    // =====================================================================
    //  PARÁMETRO K — Control de propagación
    // =====================================================================

    /**
     * Parámetro K de LRTA*(k): número máximo de nodos cuya heurística puede
     * actualizarse por tick (incluyendo el nodo actual).
     *
     * Con K=1 el algoritmo se reduce a LRTA* básico (sin propagación).
     * Valores mayores permiten propagar más actualizaciones por tick,
     * mejorando la convergencia a costa de más cómputo por tick.
     * K=5 produce resultados alineados con el solucionario de la práctica.
     */
    private static final int K = 5;

    // =====================================================================
    //  ATRIBUTOS DEL MAPA (inicializados una vez en el constructor)
    // =====================================================================

    /** Tamaño en píxeles de cada celda de la cuadrícula del juego. */
    private int blockSize;

    /** Dimensiones de la cuadrícula del mapa (ancho x alto en celdas). */
    private int gridW, gridH;

    /** Coordenadas (en celdas) del portal de salida (meta del agente). */
    private int metaX, metaY;

    /**
     * Matrices booleanas de obstáculos.
     * muro[x][y] = true bloquea el paso al caminar y detiene el vuelo.
     * agua[x][y] = true también bloquea pero mata al agente si cae en vuelo.
     */
    private boolean[][] muro, agua;

    /**
     * Mapa de catapultas: posición codificada (long) → vector de lanzamiento {dx, dy}.
     */
    private HashMap<Long, int[]> catDir;

    /**
     * Índice bitmask de cada catapulta: posición codificada → índice 0..numCats-1.
     */
    private HashMap<Long, Integer> catIdx;

    /** Número total de catapultas en el mapa. */
    private int numCats;

    /** Posiciones codificadas de cada moneda. */
    private long[] monPos;

    /** Número total de monedas. */
    private int numMon;

    /** Posiciones codificadas de cada llave. */
    private long[] llavePos;

    /** Número total de llaves. */
    private int numLlaves;

    /**
     * Si true, las catapultas no cuestan monedas (mapa sin monedas).
     */
    private boolean catapultasGratis;

    /** Coordenadas iniciales del avatar (en celdas). */
    private int iniX, iniY;

    // =====================================================================
    //  ESTRUCTURAS DE LRTA*(k) — Tabla heurística y soporte
    // =====================================================================

    /**
     * Tabla de heurísticas aprendidas: clave del estado → valor h(n).
     *
     * QUÉ: Almacena las heurísticas actualizadas por la regla de aprendizaje.
     * Si un estado no está en la tabla, se inicializa con la heurística base
     * (Manhattan al portal) en el primer acceso (lazy initialization).
     *
     * POR QUÉ: Persiste entre ticks, acumulando el aprendizaje. Los valores
     * de h(n) se acercan progresivamente a h*(n) (coste real óptimo) a medida
     * que el agente explora el espacio de estados.
     */
    private HashMap<String, Double> tablaH;

    /**
     * Tabla de soportes: clave del estado → clave de su mejor vecino.
     *
     * QUÉ: Para cada estado x que ha sido procesado por LookaheadUpdateK,
     * almacena la clave del vecino y que minimiza c(x,y) + h(y).
     *
     * POR QUÉ: Se usa en la fase de propagación para decidir si un vecino z
     * debe ser actualizado. Solo se propaga a z si soporte(z) == x, es decir,
     * si el mejor camino de z pasaba por x (que acaba de cambiar su h).
     * Esto hace que la propagación sea selectiva y eficiente: solo se
     * actualizan nodos realmente afectados.
     */
    private HashMap<String, String> soporte;

    // =====================================================================
    //  MÉTRICAS DE LA BÚSQUEDA
    // =====================================================================

    /** Nodos expandidos (uno por tick, igual que en RTA*). */
    private int nodosExp = 0;

    /** Número de acciones ejecutadas por el agente. */
    private int numAcciones = 0;

    /**
     * Número de veces que se ha actualizado un valor en tablaH.
     * Se incrementa cada vez que h(x) < minF y se escribe el nuevo valor.
     * Esta métrica es específica de LRTA*(k) y no existe en RTA*.
     */
    private int numActualizacionesTabla = 0;

    /** Marca temporal (ms) del inicio de la búsqueda. */
    private long tiempoInicio;

    /** Flag que indica si ya se han reportado las métricas finales. */
    private boolean haTerminado = false;

    /** Nodo actual del agente en el modelo interno del juego. */
    private Nodo actual;

    /**
     * Orden fijo de expansión: RIGHT, UP, LEFT, DOWN.
     * Determina el desempate en argmin: ante igualdad de f, se elige
     * el primer sucesor según este orden.
     */
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT,
        ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,
        ACTIONS.ACTION_DOWN
    };


    // =====================================================================
    //  CONSTRUCTOR — Parseo completo del mapa
    // =====================================================================

    /**
     * Constructor del agente LRTA*(k).
     *
     * QUÉ HACE: Parsea toda la información estática del mapa una sola vez:
     * portal, muros, agua, catapultas (con dirección e índice bitmask),
     * monedas y llaves. Inicializa las tablas de heurística y soporte vacías.
     *
     * CÓMO LO HACE: Idéntico al constructor de AgenteRTAStar:
     * 1. Dimensiones de la cuadrícula y posición del avatar.
     * 2. Localización del portal.
     * 3. Clasificación de inmóviles por itype (0=muro, 3=agua, 5-8=catapultas).
     * 4. Clasificación de recursos (15=moneda, 16=llave, excluyendo avatar).
     * 5. Determinación de catapultasGratis.
     *
     * NOTA: A diferencia de RTA*, el estado actual NO se inicializa en el
     * constructor sino en el primer tick de act(). Esto es una decisión de
     * implementación que no afecta al comportamiento.
     *
     * @param so    Estado inicial del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU (requerido por la interfaz).
     */
    public AgenteLRTAStarK(StateObservation so, ElapsedCpuTimer timer) {
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

        // --- Tablas de aprendizaje LRTA*(k) ---
        tablaH = new HashMap<>();
        soporte = new HashMap<>();

        // --- Localizar portal de salida (meta) ---
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
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;

                    if (obs.itype == 0) {
                        muro[x][y] = true;
                    } else if (obs.itype == 3) {
                        agua[x][y] = true; muro[x][y] = true;
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

        // --- Posición inicial del avatar ---
        Vector2d ap = so.getAvatarPosition();
        iniX = gx(ap); iniY = gy(ap);

        // --- Clasificar recursos: monedas y llaves ---
        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Long> kl = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) ml.add(enc(gx(obs.position), gy(obs.position)));
                    else if (obs.itype == 16) {
                        int kx = gx(obs.position), ky = gy(obs.position);
                        // Excluir posición del avatar (GVGAI recoge automáticamente)
                        if (kx != iniX || ky != iniY) kl.add(enc(kx, ky));
                    }
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

        // Registrar el inicio del temporizador
        tiempoInicio = System.currentTimeMillis();
    }


    // =====================================================================
    //  ACT — Bucle principal de LRTA*(k) (pág. 32 del curso)
    // =====================================================================
    //
    //  Pseudocódigo:
    //    actual = nodo_inicial
    //    forall nodo: soporte(nodo) = null
    //    while True:
    //        if actual == objetivo: break
    //        LookaheadUpdateK(actual, k)            ← paso 1
    //        mejorVecino = argmin{c(actual,y)+h(y)} ← paso 2
    //        actual = mejorVecino                    ← paso 3
    //
    // =====================================================================

    /**
     * Método llamado por GVGAI en cada tick del juego.
     *
     * QUÉ HACE: Ejecuta un paso del bucle principal de LRTA*(k):
     * 1. Llama a LookaheadUpdateK para actualizar h del nodo actual y
     *    propagar la actualización a hasta k nodos afectados.
     * 2. Selecciona el mejor sucesor (argmin f con la h ya actualizada).
     * 3. Mueve el agente al mejor sucesor.
     *
     * POR QUÉ PRIMERO ACTUALIZAR Y LUEGO ELEGIR: El orden es crucial.
     * LookaheadUpdateK modifica h(actual) y potencialmente h de vecinos,
     * por lo que la selección del mejor sucesor DEBE hacerse después para
     * usar los valores heurísticos más recientes. Si se eligiera antes de
     * actualizar, el agente tomaría decisiones con información obsoleta.
     *
     * INICIALIZACIÓN DIFERIDA DEL ESTADO: El estado actual se crea en el
     * primer tick (no en el constructor) porque la inicialización de los
     * bitmasks depende de numMon, numLlaves y numCats, que ya están
     * establecidos al final del constructor.
     *
     * REPORTE DE MÉTRICAS: Se comprueba la meta tanto antes de actuar como
     * después de moverse. La comprobación post-movimiento es necesaria
     * porque GVGAI puede terminar la partida antes del siguiente tick
     * (especialmente en el Mapa 6), impidiendo que se ejecute el siguiente
     * act(). Se usa también result() como red de seguridad.
     *
     * @param so    Estado actual del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU.
     * @return La acción a ejecutar en este tick.
     */
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        if (haTerminado) return ACTIONS.ACTION_NIL;

        // Contadores de métricas: un nodo expandido y una acción por tick
        numAcciones++;
        nodosExp++;

        // --- Inicialización diferida del estado actual (primer tick) ---
        // Se crea aquí con todos los bitmasks activos (todos los recursos disponibles)
        if (actual == null) {
            actual = new Nodo(iniX, iniY, 0, false,
                (1 << numMon) - 1,      // Todas las monedas disponibles
                (1 << numLlaves) - 1,   // Todas las llaves disponibles
                (1 << numCats) - 1,     // Todas las catapultas disponibles
                0, 0, 0);               // Fase 0, sin vuelo
        }

        // --- Comprobar meta antes de actuar ---
        // Pseudocódigo: "if actual == objetivo: break"
        if (esMeta(actual)) {
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setNumAccionesPlan(numAcciones);
            mp.setNumActualizacionesTabla(numActualizacionesTabla);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - tiempoInicio);
            mp.setAgente("LRTA*(k)");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // --- Paso 1: Actualizar heurística con propagación ---
        // Pseudocódigo: "LookaheadUpdateK(actual, k)"
        // Esto actualiza h(actual) y potencialmente hasta k-1 vecinos afectados
        lookaheadUpdateK(actual, K);

        // --- Paso 2: Elegir mejor sucesor con h ya actualizada ---
        // Pseudocódigo: "mejorVecino = argmin{c(actual,y) + h(y)}"
        // Se recorren las acciones en ORDEN (R,U,L,D); ante empate de f
        // se queda el primero encontrado (desempate por orden de expansión)
        double mejorF = Double.MAX_VALUE;
        ACTIONS mejorAccion = ACTIONS.ACTION_NIL;
        Nodo mejorSucesor = null;

        ACTIONS[] accionesDisp = (actual.fase == 0) ? ORDEN
            : new ACTIONS[]{ACTIONS.ACTION_NIL};

        for (ACTIONS a : accionesDisp) {
            Nodo suc = trans(actual, a);
            if (suc == null) continue;
            double f = 1.0 + obtenerH(suc);  // f = c(actual, suc) + h(suc), con c=1
            if (f < mejorF) {
                mejorF = f;
                mejorAccion = a;
                mejorSucesor = suc;
            }
        }

        // Caso sin sucesores: agente atrapado
        if (mejorSucesor == null) {
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setNumAccionesPlan(numAcciones);
            mp.setNumActualizacionesTabla(numActualizacionesTabla);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - tiempoInicio);
            mp.setAgente("LRTA*(k)");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // --- Paso 3: Moverse al mejor vecino ---
        // Pseudocódigo: "actual = mejorVecino"
        actual = mejorSucesor;


        // Comprobar meta tras moverse (para reportar métricas a tiempo;
        // GVGAI puede terminar antes del siguiente tick, especialmente en Mapa 6)
        if (esMeta(actual)) {
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExp);
            mp.setNumAccionesPlan(numAcciones);
            mp.setNumActualizacionesTabla(numActualizacionesTabla);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - tiempoInicio);
            mp.setAgente("LRTA*(k)");
            mp.printMetrics();
        }

        return mejorAccion;
    }


    // =====================================================================
    //  PROPAGACIÓN LRTA*(k) — LookaheadUpdateK (pág. 33 del curso)
    // =====================================================================
    //
    //  Pseudocódigo:
    //    PROCEDURE LookaheadUpdateK(actual, k):
    //        Cola = <actual>
    //        contador = k - 1
    //        while !Cola.empty():
    //            x = Cola.pop()
    //            mejorVecinoX = argmin { c(x,y) + h(y) }
    //            soporte(x) = mejorVecinoX
    //            propagar = False
    //            if h(x) < c(x, mejorVecinoX) + h(mejorVecinoX):
    //                propagar = True
    //                h(x) = c(x, mejorVecinoX) + h(mejorVecinoX)
    //            if propagar:
    //                foreach sucesor in Sucesores(x):
    //                    if contador > 0 and soporte(sucesor) == x:
    //                        Cola.insert(sucesor)
    //                        contador = contador - 1
    //
    // =====================================================================

    /**
     * Procedimiento de actualización con propagación acotada de LRTA*(k).
     *
     * QUÉ HACE: Actualiza la heurística del nodo de inicio y propaga esa
     * actualización a hasta k nodos vecinos afectados, siguiendo el
     * pseudocódigo de las diapositivas (pág. 33).
     *
     * CÓMO FUNCIONA:
     * 1. Se encola el nodo de inicio y se inicializa un contador = k-1
     *    (el nodo de inicio ya cuenta como una de las k actualizaciones).
     *
     * 2. Para cada nodo x que se extrae de la cola:
     *    a) Se calcula su mejor vecino (argmin f) y se registra como soporte(x).
     *    b) Se aplica la regla de aprendizaje de LRTA*:
     *       Si h(x) < minF → h(x) = minF (actualización al alza).
     *       Se incrementa numActualizacionesTabla.
     *    c) Si hubo actualización (propagar = true), se recorren los sucesores
     *       de x y se encolan aquellos cuyo soporte sea x (es decir, nodos
     *       que dependen de x como mejor camino), siempre que quede presupuesto
     *       de propagación (contador > 0).
     *
     * POR QUÉ SE USA UNA COLA (BFS): La propagación sigue un esquema de
     * "ondas": primero se actualiza el nodo actual, luego sus vecinos directos
     * afectados, luego los vecinos de esos vecinos, etc. La cola FIFO
     * garantiza este orden por niveles.
     *
     * POR QUÉ SE USA EL PRIMER MÍNIMO (DIFERENCIA CON RTA*):
     * La regla de aprendizaje h(x) = min{c(x,y) + h(y)} usa el primer
     * mínimo, no el segundo como en RTA*. Esto mantiene h(n) admisible
     * (nunca sobreestima), lo que es apropiado para un algoritmo de
     * aprendizaje que converge a h*(n) en múltiples ejecuciones.
     *
     * POR QUÉ SOLO SE PROPAGA SI soporte(sucesor) == x: Si el mejor
     * camino de un sucesor z no pasaba por x, entonces el cambio en h(x)
     * no afecta a z. Solo los nodos cuyo mejor vecino (soporte) sea x
     * pueden verse afectados y necesitar actualización.
     *
     * @param inicio  Estado desde el que iniciar la propagación (normalmente el actual).
     * @param limiteK Parámetro k: número máximo de nodos actualizables.
     */
    private void lookaheadUpdateK( Nodo inicio, int limiteK) {
        // Cola BFS para propagar actualizaciones
        Queue<Nodo> cola = new LinkedList<>();
        cola.add(inicio);
        int contador = limiteK - 1;  // Presupuesto restante de propagación

        while (!cola.isEmpty()) {
            Nodo x = cola.poll();

            // --- Calcular mejor vecino de x (soporte) ---
            // mejorVecinoX = argmin { c(x,y) + h(y) | y ∈ Sucesores(x) }
            double minF = Double.MAX_VALUE;
            Nodo mejorVecino = null;

            ACTIONS[] acciones = (x.fase == 0) ? ORDEN
                : new ACTIONS[]{ACTIONS.ACTION_NIL};

            for (ACTIONS a : acciones) {
                Nodo vecino = trans(x, a);
                if (vecino == null) continue;
                double f = 1.0 + obtenerH(vecino);  // c(x, vecino) = 1
                if (f < minF) {
                    minF = f;
                    mejorVecino = vecino;
                }
            }

            // Sin vecinos válidos: no se puede actualizar ni propagar
            if (mejorVecino == null) continue;

            // --- Registrar soporte(x) = mejorVecino ---
            soporte.put(x.key(), mejorVecino.key());

            // --- Regla de aprendizaje de LRTA* ---
            // if h(x) < c(x, mejorVecinoX) + h(mejorVecinoX):
            //     h(x) = c(x, mejorVecinoX) + h(mejorVecinoX)
            boolean propagar = false;
            double hActual = obtenerH(x);
            if (hActual < minF) {
                propagar = true;
                tablaH.put(x.key(), minF);
                numActualizacionesTabla++;  // Métrica específica de LRTA*(k)
            }

            // --- Fase de propagación: encolar vecinos afectados ---
            // Solo si hubo actualización (propagar == true)
            // Solo vecinos cuyo soporte sea x (soporte(sucesor) == x)
            // Solo si queda presupuesto (contador > 0)
            if (propagar) {
                for (ACTIONS a : acciones) {
                    Nodo sucesor = trans(x, a);
                    if (sucesor == null) continue;
                    // Comprobar si el soporte del sucesor es x
                    // (es decir, el mejor camino del sucesor pasaba por x)
                    if (contador > 0 && x.key().equals(soporte.get(sucesor.key()))) {
                        cola.add(sucesor);
                        contador--;  // Consumir presupuesto de propagación
                    }
                }
            }
        }
    }


    // =====================================================================
    //  TABLA HEURÍSTICA — Consulta con inicialización bajo demanda
    // =====================================================================

    /**
     * Obtiene h(n) de un estado, consultando la tabla de heurísticas aprendidas.
     *
     * Si el estado ya tiene un valor aprendido, lo devuelve.
     * Si no, calcula la heurística base (Manhattan al portal), la almacena
     * para futuras consultas (lazy initialization) y la devuelve.
     *
     * @param n Nodo del que obtener h(n).
     * @return Valor heurístico, posiblemente actualizado por LookaheadUpdateK.
     */
    private double obtenerH(Nodo n) {
        String k = n.key();
        if (tablaH.containsKey(k)) return (double) tablaH.get(k);
        double h0 = heuristica(n);
        tablaH.put(k, h0);
        return h0;
    }

    /**
     * Heurística base: distancia Manhattan entre el agente y el portal.
     * Admisible (nunca sobreestima) y consistente con los demás agentes.
     *
     * @param n Nodo del que calcular la heurística.
     * @return Distancia Manhattan al portal ≥ 0.
     */
    private double heuristica(Nodo n) {
        return Math.abs(n.x - metaX) + Math.abs(n.y - metaY);
    }

    /**
     * Condición de meta: agente en el portal, con la llave y en fase 0.
     * GVGAI requiere fase 0 (caminando) para reconocer victoria.
     */
    private boolean esMeta(Nodo n) {
        return n.x == metaX && n.y == metaY && n.llave && n.fase == 0;
    }


    // =====================================================================
    //  FUNCIÓN DE TRANSICIÓN — Motor de física del juego
    // =====================================================================

    /**
     * Calcula el estado resultante de aplicar una acción a un estado.
     * Idéntica a la de AgenteAStar y AgenteRTAStar (mismo modelo del mundo).
     *
     * FASE 0 — Caminando: valida movimiento, recoge recursos, detecta catapultas.
     * FASE 1 — Tick de activación de catapulta → transiciona a fase 2.
     * FASE 2 — En vuelo: avanza, detecta colisiones/recursos/encadenamiento.
     * FASE 3 — Rebote en catapulta → transiciona a fase 2 con nueva dirección.
     *
     * @param n Nodo actual.
     * @param a Acción a aplicar.
     * @return Nuevo nodo, o null si la acción es inválida o mortal.
     */
    private Nodo trans(Nodo n, ACTIONS a) {
        // --- FASE 0: Caminando ---
        if (n.fase == 0) {
            if (a == ACTIONS.ACTION_NIL) return null;
            int[] d = delta(a);
            int nx = n.x + d[0], ny = n.y + d[1];
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) return null;
            if (muro[nx][ny]) return null;
            if (nx == metaX && ny == metaY && !n.llave) return null;
            int m = n.mon; boolean l = n.llave; int mB = n.mB, lB = n.lB, cB = n.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) { lB &= ~(1 << li); if (!l) l = true; }
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                if (!catapultasGratis && m <= 0) return null;
                if (!catapultasGratis) m--;
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Nodo(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }
            return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        // --- FASE 1: Tick de activación ---
        } else if (n.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);

        // --- FASE 2: En vuelo ---
        } else if (n.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = n.x + n.vdx, ty = n.y + n.vdy;
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);
            if (!col && tx == metaX && ty == metaY && !n.llave) col = true;
            if (col) {
                if (agua[n.x][n.y]) return null;  // Muerte por agua
                return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 0, 0, 0);
            }
            int nx = tx, ny = ty;
            int m = n.mon; boolean l = n.llave; int mB = n.mB, lB = n.lB, cB = n.cB;
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) { lB &= ~(1 << li); if (!l) l = true; }
            if (nx == metaX && ny == metaY && l)
                return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk); cB &= ~(1 << ci);
                return new Nodo(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }
            return new Nodo(nx, ny, m, l, mB, lB, cB, 2, n.vdx, n.vdy);

        // --- FASE 3: Rebote en catapulta ---
        } else if (n.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);
        }
        return null;
    }


    // =====================================================================
    //  UTILIDADES
    // =====================================================================

    /**
     * Busca el índice de la llave en (x,y) para el bitmask lB.
     * @return Índice 0..numLlaves-1, o -1 si no hay llave.
     */
    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }

    /** Convierte coordenada X de píxeles a columna de la cuadrícula. */
    private int gx(Vector2d p) { return (int)(p.x / blockSize); }

    /** Convierte coordenada Y de píxeles a fila de la cuadrícula. */
    private int gy(Vector2d p) { return (int)(p.y / blockSize); }

    /**
     * Codifica posición (x,y) en un long único. Inyectiva para coordenadas válidas.
     */
    private long enc(int x, int y) { return (long)y * gridW + x; }

    /**
     * Desplazamiento {dx,dy} de una acción. Eje Y crece hacia abajo (GVGAI).
     */
    private int[] delta(ACTIONS a) {
        switch(a){
            case ACTION_RIGHT: return new int[]{1, 0};
            case ACTION_LEFT:  return new int[]{-1, 0};
            case ACTION_UP:    return new int[]{0, -1};
            case ACTION_DOWN:  return new int[]{0, 1};
            default: return new int[]{0, 0};
        }
    }

    /**
     * Busca el índice de la moneda en (x,y) para el bitmask mB.
     * @return Índice 0..numMon-1, o -1 si no hay moneda.
     */
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for(int i = 0; i < numMon; i++) if(monPos[i] == k) return i;
        return -1;
    }

    /**
     * Devuelve el vector de lanzamiento {dx,dy} según el itype de la catapulta.
     * Hardcodeado: 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT.
     */
    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0, 1};
            case 6: return new int[]{0, -1};
            case 7: return new int[]{1, 0};
            case 8: return new int[]{-1, 0};
            default: return null;
        }
    }


    // =====================================================================
    //  CLASE INTERNA — Nodo
    // =====================================================================

    /**
     * Representa un estado completo del juego para LRTA*(k).
     *
     * Codifica: posición del agente (x,y), monedas portadas (mon),
     * posesión de llave, bitmasks de recursos disponibles (mB, lB, cB),
     * fase de vuelo (0-3) y dirección de vuelo (vdx, vdy).
     *
     * Los bitmasks permiten rastrear eficientemente qué recursos quedan:
     * bit 1 = disponible, bit 0 = recogido/usado.
     * Inicialización: (1<<n)-1 activa los n bits menos significativos.
     */
    private static class Nodo {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy; boolean llave;
        Nodo(int x, int y, int m, boolean l, int mB, int lB, int cB, int f, int vx, int vy) {
            this.x=x; this.y=y; mon=m; llave=l;
            this.mB=mB; this.lB=lB; this.cB=cB; fase=f; vdx=vx; vdy=vy;
        }

        /**
         * Clave única del estado para indexar en tablaH, soporte y detectar
         * duplicados. Serializa todos los campos relevantes.
         */
        String key() {
            return x+","+y+","+(llave?1:0)+","+mB+","+lB+","+cB+","+mon+","+fase+","+vdx+","+vdy;
        }
    }
}