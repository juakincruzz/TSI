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
 * AGENTE RTA* — Búsqueda heurística en tiempo real
 * ============================================================================
 *
 * Implementación del algoritmo Real-Time A* (RTA*)
 *
 * -------------------------------------------------------------------------
 * QUÉ HACE ESTE AGENTE
 * -------------------------------------------------------------------------
 * A diferencia de A* (que planifica offline el camino completo antes de
 * moverse), RTA* toma decisiones en tiempo real: en cada tick del juego
 * examina únicamente los vecinos inmediatos del nodo actual (espacio local
 * de búsqueda), elige el mejor y se mueve a él. Simultáneamente, actualiza
 * la heurística del nodo que abandona usando el segundo mínimo de los
 * costes de sus vecinos (regla de aprendizaje de RTA*).
 *
 * -------------------------------------------------------------------------
 * CÓMO LO HACE — Pseudocódigo (diapositivas del curso, pág. 13)
 * -------------------------------------------------------------------------
 *   actual = nodo_inicial
 *   while True:
 *       if actual == objetivo: break
 *       S = sucesores(actual)
 *       foreach sucesor in S:
 *           f(sucesor) = h(sucesor) + distance(actual, sucesor)
 *       z          = argmin { f(y) | y ∈ S }
 *       segundo_min = Segundo_Mínimo({ f(y) | y ∈ S })
 *       h(actual)  = max( h(actual), segundo_min )
 *       actual     = z
 *
 * Cada iteración del bucle corresponde a un tick del juego (una llamada a
 * act()). El agente expande exactamente un nodo por tick, por lo que el
 * número de nodos expandidos coincide con el número de timesteps.
 *
 * -------------------------------------------------------------------------
 * POR QUÉ SE DISEÑA ASÍ
 * -------------------------------------------------------------------------
 * - RTA* es una búsqueda en tiempo real: decide en tiempo constante por
 *   tick (solo mira los vecinos del nodo actual), lo que lo hace apto para
 *   entornos que exigen movimientos rápidos o donde no se puede permitir
 *   el coste computacional de una búsqueda offline completa.
 *
 * - La regla de aprendizaje con el SEGUNDO MÍNIMO puede sobreestimar h(n),
 *   lo que permite al agente escapar más rápido de mínimos locales de la
 *   heurística (zonas que parecen prometedoras pero no llevan a la meta).
 *   Esto contrasta con LRTA*, que usa el primer mínimo y por tanto
 *   mantiene h(n) admisible pero escapa más lento de mínimos locales.
 *
 * - No garantiza optimalidad del camino (a diferencia de A*), pero permite
 *   al agente moverse sin necesidad de explorar todo el espacio de estados.
 *
 * - La heurística base es Manhattan al portal (sin considerar llave),
 *   consistente con los demás agentes de la práctica.
 *
 * -------------------------------------------------------------------------
 * DESEMPATE Y ORDEN DE ACCIONES
 * -------------------------------------------------------------------------
 * Al seleccionar el mejor vecino (argmin f), en caso de empate se elige
 * el primero según el orden de iteración: RIGHT, UP, LEFT, DOWN.
 * Este orden reproduce el convenio de las diapositivas del curso y asegura
 * resultados reproducibles frente al solucionario.
 *
 * -------------------------------------------------------------------------
 * CATAPULTAS
 * -------------------------------------------------------------------------
 * Detección por itype hardcodeado: 5=DOWN, 6=UP, 7=RIGHT, 8=LEFT.
 * El sistema de 4 fases de vuelo (0=normal, 1=catapulta, 2=vuelo,
 * 3=rebote) es idéntico al de AgenteAStar, modelando fielmente la
 * mecánica de vuelo de GVGAI tick a tick.
 *
 * @author Joaquín Cruz Lorenzo
 */
public class AgenteRTAStar extends AbstractPlayer {

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
     * muro[x][y] = true si la celda bloquea el paso al caminar.
     * agua[x][y] = true si la celda es agua (también bloquea, pero la mecánica
     * de colisión en vuelo difiere: caer sobre agua mata al agente, mientras
     * que un muro sólido simplemente detiene el vuelo).
     */
    private boolean[][] muro, agua;

    /**
     * Mapa de catapultas: posición codificada (long) → vector de lanzamiento {dx, dy}.
     * Se usa HashMap para acceso O(1) por posición.
     */
    private HashMap<Long, int[]> catDir;

    /**
     * Índice de cada catapulta para el bitmask del estado.
     * Posición codificada (long) → índice 0..numCats-1 (posición de bit en cB).
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

    /**
     * Si true, las catapultas no cuestan monedas (ocurre cuando numMon == 0).
     * Es una mecánica del juego que debe manejarse explícitamente.
     */
    private boolean catapultasGratis;

    // =====================================================================
    //  ATRIBUTOS DE LA BÚSQUEDA EN TIEMPO REAL
    // =====================================================================

    /**
     * Estado actual del agente en el modelo interno.
     * En RTA* el agente se "mueve" por el espacio de estados tick a tick,
     * actualizando este campo en cada llamada a act().
     */
    private Nodo actual;

    /**
     * Tabla de heurísticas aprendidas: clave del estado → valor h(n).
     *
     * QUÉ: Almacena las heurísticas actualizadas por la regla de aprendizaje
     * de RTA*. Si un estado no está en la tabla, se usa la heurística base
     * (Manhattan al portal).
     *
     * POR QUÉ: La tabla persiste entre ticks, permitiendo que las
     * actualizaciones de h(n) acumuladas en pasos anteriores influyan en
     * decisiones futuras. Esto es clave para que RTA* pueda escapar de
     * mínimos locales: al sobreestimar h(n) de estados ya visitados,
     * el agente es "empujado" hacia zonas no exploradas.
     */
    private HashMap<String, Double> tablaH;

    /**
     * Contador de nodos expandidos.
     * En RTA* cada tick expande exactamente un nodo (el actual), por lo que
     * nodosExp == número de timesteps transcurridos.
     */
    private int nodosExp = 0;

    /** Flag que indica si la búsqueda ha terminado (meta alcanzada o sin salida). */
    private boolean finalizado = false;

    /** Marca temporal (ms) del inicio de la búsqueda para medir tiempo total. */
    private long t0 = -1;

    /**
     * Orden fijo de expansión de acciones en fase 0 (caminando).
     * RIGHT, UP, LEFT, DOWN — siguiendo el convenio del pseudocódigo del curso.
     * Determina el desempate implícito en argmin: ante igualdad de f, se
     * elige la primera acción en este orden.
     */
    private static final ACTIONS[] ORDEN = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT, ACTIONS.ACTION_DOWN
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
     * constructor es lento e inestable (causa de bugs en iteraciones previas).
     *
     * @param itype Tipo del objeto inmóvil según GVGAI.
     * @return Vector {dx, dy} de lanzamiento, o null si no es catapulta.
     */
    private static int[] catapultDir(int itype) {
        switch (itype) {
            case 5: return new int[]{0,  1};   // DOWN
            case 6: return new int[]{0, -1};   // UP
            case 7: return new int[]{1,  0};   // RIGHT
            case 8: return new int[]{-1, 0};   // LEFT
            default: return null;
        }
    }


    // =====================================================================
    //  CONSTRUCTOR — Parseo completo del mapa
    // =====================================================================

    /**
     * Constructor del agente RTA*.
     *
     * QUÉ HACE: Lee toda la información estática del mapa una sola vez:
     * dimensiones, posición del avatar, portal, muros, agua, catapultas
     * (con dirección e índice bitmask), monedas y llaves. Además, inicializa
     * el estado actual del agente y la tabla de heurísticas vacía.
     *
     * CÓMO LO HACE:
     * 1. Extrae dimensiones de la cuadrícula y posición del avatar.
     * 2. Localiza el portal de salida (meta).
     * 3. Recorre objetos inmóviles clasificándolos por itype:
     *    - itype 0  → muro sólido.
     *    - itype 3  → agua (bloquea y mata en vuelo).
     *    - itype 5-8 → catapultas (se registran con dirección e índice bitmask).
     * 4. Recorre recursos:
     *    - itype 15 → moneda.
     *    - itype 16 → llave (excluyendo posición del avatar).
     * 5. Determina catapultasGratis (mapa sin monedas).
     * 6. Crea el estado inicial con todos los bitmasks activos.
     *
     * POR QUÉ EN EL CONSTRUCTOR: La información del mapa es estática, así que
     * se parsea una sola vez para no repetir trabajo en cada tick. Esto es
     * especialmente importante en RTA* porque act() se llama muchas veces
     * (una por tick) y debe ser lo más rápido posible.
     *
     * @param so    Estado inicial del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU (requerido por la interfaz).
     */
    public AgenteRTAStar(StateObservation so, ElapsedCpuTimer timer) {
        super();

        // --- Dimensiones de la cuadrícula ---
        blockSize = so.getBlockSize();
        gridW = so.getObservationGrid().length;
        gridH = so.getObservationGrid()[0].length;

        // --- Matrices de obstáculos ---
        muro  = new boolean[gridW][gridH];
        agua  = new boolean[gridW][gridH];

        // --- Estructuras para catapultas y tabla heurística ---
        catDir = new HashMap<>();
        catIdx = new HashMap<>();
        tablaH = new HashMap<>();

        // --- Posición inicial del avatar (de píxeles a celdas) ---
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
                    if (x < 0 || x >= gridW || y < 0 || y >= gridH) continue;
                    if (obs.itype == 0) {
                        muro[x][y] = true;
                    } else if (obs.itype == 3) {
                        agua[x][y] = true;
                        muro[x][y] = true;  // Agua también bloquea el paso al caminar
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

        // --- Clasificar recursos: monedas y llaves ---
        ArrayList<Long> ml = new ArrayList<>();
        ArrayList<Long> kl = new ArrayList<>();
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    if (obs.itype == 15) {
                        ml.add(enc(gx(obs.position), gy(obs.position)));
                    } else if (obs.itype == 16) {
                        int kx = gx(obs.position), ky = gy(obs.position);
                        // Excluir posición del avatar (si empieza sobre una llave,
                        // GVGAI la recoge automáticamente)
                        if (kx != iniX || ky != iniY)
                            kl.add(enc(kx, ky));
                    }
                }
            }
        }

        // Convertir listas a arrays para acceso rápido
        numMon = ml.size();
        monPos = new long[numMon];
        for (int i = 0; i < numMon; i++) monPos[i] = ml.get(i);
        numLlaves = kl.size();
        llavePos  = new long[numLlaves];
        for (int i = 0; i < numLlaves; i++) llavePos[i] = kl.get(i);
        catapultasGratis = (numMon == 0);

        // --- Estado inicial del agente ---
        // Posición del avatar, 0 monedas, sin llave, todos los bitmasks activos
        // (todos los recursos disponibles), fase 0, sin velocidad de vuelo.
        actual = new Nodo(iniX, iniY, 0, false,
                (1 << numMon)    - 1,   // Todas las monedas disponibles
                (1 << numLlaves) - 1,   // Todas las llaves disponibles
                (1 << numCats)   - 1,   // Todas las catapultas disponibles
                0, 0, 0);               // Fase 0, sin vuelo
    }


    // =====================================================================
    //  ACT — Implementación fiel al pseudocódigo RTA* (pág. 13 del curso)
    // =====================================================================
    //
    //  Pseudocódigo RTA* (Korf, 1990):
    //
    //  actual = nodo_inicial
    //  while True:
    //    if actual == objetivo: break
    //    S = sucesores(actual)
    //    foreach sucesor in S:
    //      f(sucesor) = h(sucesor) + distance(actual, sucesor)
    //    z          = argmin { f(y) | y ∈ S }
    //    segundo_min = Segundo_Mínimo({ f(y) | y ∈ S })
    //    h(actual)  = max( h(actual), segundo_min )
    //    actual     = z
    //
    // =====================================================================

    /**
     * Método llamado por GVGAI en cada tick del juego.
     *
     * QUÉ HACE: Ejecuta un paso del bucle RTA*:
     * 1. Comprueba si el estado actual es meta → si lo es, finaliza.
     * 2. Genera los sucesores válidos del estado actual.
     * 3. Calcula f(sucesor) = h(sucesor) + 1 para cada sucesor.
     * 4. Selecciona el mejor sucesor z (argmin f) con desempate por orden.
     * 5. Calcula el segundo mínimo de los f(sucesor).
     * 6. Actualiza h(actual) = max(h(actual), segundo_min)  [regla de aprendizaje].
     * 7. Mueve el agente al mejor sucesor z.
     *
     * POR QUÉ UN PASO POR TICK: RTA* es una búsqueda en tiempo real.
     * La clave de su diseño es que cada tick tiene coste computacional
     * constante (solo se examinan los vecinos inmediatos), lo que permite
     * actuar rápidamente. Esto se consigue expandiendo exactamente un nodo
     * por tick, que es el nodo actual.
     *
     * POR QUÉ SE USA EL SEGUNDO MÍNIMO: Es la regla de aprendizaje
     * definitoria de RTA* (frente a LRTA*, que usa el primer mínimo).
     * Al usar el segundo mínimo, h(actual) puede sobreestimar el coste
     * real, lo que hace que el agente "rechace" volver a estados ya
     * visitados con más fuerza. Esto permite escapar más rápido de
     * mínimos locales de la heurística en una sola ejecución.
     *
     * CASO ESPECIAL — UN SOLO SUCESOR: Si solo hay un sucesor, el segundo
     * mínimo coincide con el primer mínimo (no hay otro candidato).
     *
     * CASO ESPECIAL — SIN SUCESORES: Si no hay sucesores válidos, el agente
     * está atrapado (callejón sin salida) y se finaliza la búsqueda.
     *
     * @param so    Estado actual del juego proporcionado por GVGAI.
     * @param timer Temporizador de CPU.
     * @return La acción a ejecutar en este tick.
     */
    @Override
    public ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
        // Si ya se finalizó, no hacer nada
        if (finalizado) return ACTIONS.ACTION_NIL;

        // Iniciar temporizador en el primer tick
        if (t0 < 0) t0 = System.currentTimeMillis();

        // Cada tick expande exactamente un nodo (el actual)
        nodosExp++;

        // --- Paso 1: Comprobar si el estado actual es meta ---
        // Según el pseudocódigo: "if actual == objetivo: break"
        if (esMeta(actual)) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // --- Paso 2: Generar sucesores válidos (S = sucesores(actual)) ---
        // En fase 0 se prueban las 4 direcciones; en fases de vuelo solo NIL
        ACTIONS[] acciones = (actual.fase == 0) ? ORDEN
                : new ACTIONS[]{ACTIONS.ACTION_NIL};

        // Listas paralelas: acción, nodo sucesor, y su coste f
        List<ACTIONS> accionesValidas  = new ArrayList<>();
        List<Nodo>  sucesoresValidos = new ArrayList<>();
        List<Double>  costesF         = new ArrayList<>();

        // --- Paso 3: Calcular f(sucesor) = h(sucesor) + distance(actual, sucesor) ---
        // distance(actual, sucesor) = 1 para todas las acciones (coste uniforme)
        for (ACTIONS a : acciones) {
            Nodo suc = trans(actual, a);
            if (suc == null) continue;  // Acción inválida (muro, fuera de mapa, etc.)
            double fSuc = getH(suc) + 1.0;
            accionesValidas.add(a);
            sucesoresValidos.add(suc);
            costesF.add(fSuc);
        }

        // Caso especial: sin sucesores válidos → agente atrapado
        if (accionesValidas.isEmpty()) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
            return ACTIONS.ACTION_NIL;
        }

        // --- Paso 4: Seleccionar el mejor sucesor z = argmin f(y) ---
        // Desempate implícito: al iterar en orden R, U, L, D y usar '<' estricto,
        // ante igualdad de f se conserva el primero encontrado (orden del array ORDEN)
        int mejorIdx = 0;
        double mejorF = costesF.get(0);
        for (int i = 1; i < costesF.size(); i++) {
            if (costesF.get(i) < mejorF) {
                mejorF   = costesF.get(i);
                mejorIdx = i;
            }
        }

        // --- Paso 5: Calcular el segundo mínimo de { f(y) | y ∈ S } ---
        // Si solo hay un sucesor, segundo_min = f de ese único sucesor
        // (no hay alternativa, así que se usa su propio coste).
        // Si hay más de uno, se ordena la lista y se toma el segundo elemento.
        double segundoMin;
        if (costesF.size() == 1) {
            segundoMin = costesF.get(0);
        } else {
            List<Double> sorted = new ArrayList<>(costesF);
            Collections.sort(sorted);
            segundoMin = sorted.get(1);  // Segundo menor valor
        }

        // --- Paso 6: Actualizar h(actual) — regla de aprendizaje de RTA* ---
        // h(actual) = max( h(actual), segundo_min )
        //
        // POR QUÉ: Esta actualización "eleva" la heurística del estado actual
        // para reflejar mejor el coste real de alcanzar la meta desde aquí.
        // Al usar el segundo mínimo (no el primero), se puede sobreestimar h,
        // haciendo que el agente evite regresar a este estado en el futuro.
        // Esto es lo que permite a RTA* escapar de mínimos locales.
        String keyActual = keyNodo(actual);
        Double hActual = tablaH.get(keyActual);
        if (hActual == null) hActual = heuristicaBase(actual);
        tablaH.put(keyActual, Math.max(hActual, segundoMin));

        // --- Paso 7: Moverse al mejor vecino — actual = z ---
        actual = sucesoresValidos.get(mejorIdx);

        // Comprobar si el nuevo estado es meta (para reportar métricas
        // inmediatamente y no esperar al siguiente tick, ya que GVGAI
        // puede terminar la partida antes de la siguiente llamada a act())
        if (esMeta(actual)) {
            finalizado = true;
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNumAccionesPlan(nodosExp);
            mp.setNodosExpandidos(nodosExp);
            mp.setTiempoMilisegundos(System.currentTimeMillis() - t0);
            mp.setAgente("RTA*");
            mp.printMetrics();
        }

        // Devolver la acción que lleva al mejor sucesor
        return accionesValidas.get(mejorIdx);
    }


    // =====================================================================
    //  TABLA HEURÍSTICA — Consulta con aprendizaje
    // =====================================================================

    /**
     * Obtiene el valor heurístico h(n) de un estado, consultando la tabla
     * de heurísticas aprendidas.
     *
     * QUÉ: Si el estado ya tiene un valor aprendido en tablaH, lo devuelve.
     * Si no, calcula la heurística base (Manhattan al portal), la almacena
     * en la tabla para futuras consultas, y la devuelve.
     *
     * POR QUÉ SE ALMACENA AL CONSULTAR: Inicializar bajo demanda (lazy
     * initialization) evita precalcular h para todos los posibles estados
     * del espacio (que puede ser enorme). Solo se almacenan los estados
     * que el agente realmente visita o evalúa como vecinos.
     *
     * @param n Nodo del que obtener h(n).
     * @return Valor heurístico h(n), posiblemente actualizado por la regla de aprendizaje.
     */
    private double getH(Nodo n) {
        String k = keyNodo(n);
        Double v = tablaH.get(k);
        if (v != null) return v;
        double h = heuristicaBase(n);
        tablaH.put(k, h);
        return h;
    }

    /**
     * Heurística base: distancia Manhattan entre el agente y el portal.
     *
     * Es la estimación inicial antes de cualquier aprendizaje.
     * Admisible (nunca sobreestima) porque cada paso desplaza exactamente
     * una celda en una dirección cardinal. Se usa Manhattan pura al portal
     * sin considerar la llave, lo que es consistente con los demás agentes
     * de la práctica y produce resultados alineados con el solucionario.
     *
     * @param n Nodo del que calcular la heurística base.
     * @return Distancia Manhattan al portal ≥ 0.
     */
    private double heuristicaBase(Nodo n) {
        return Math.abs(n.x - metaX) + Math.abs(n.y - metaY);
    }

    /**
     * Condición de meta: agente en el portal, con la llave y en fase 0.
     *
     * Se requiere fase 0 porque GVGAI no reconoce victoria si el agente
     * está en pleno vuelo de catapulta (fases 1-3).
     *
     * @param n Nodo a evaluar.
     * @return true si es nodo meta.
     */
    private boolean esMeta(Nodo n) {
        return n.x == metaX && n.y == metaY && n.llave && n.fase == 0;
    }


    // =====================================================================
    //  FUNCIÓN DE TRANSICIÓN — Motor de física del juego
    // =====================================================================

    /**
     * Calcula el estado resultante de aplicar una acción a un estado dado.
     *
     * La función de transición es idéntica a la de AgenteAStar, ya que
     * ambos agentes operan sobre el mismo modelo del mundo. Modela
     * fielmente las 4 fases de la mecánica del juego:
     *
     * FASE 0 — CAMINANDO (el agente elige dirección):
     *   - Valida límites, muros y portal sin llave.
     *   - Recoge monedas (máx. 5) y llaves (vía bitmasks).
     *   - Si entra en catapulta disponible: paga moneda y pasa a fase 1.
     *
     * FASE 1 — SOBRE LA CATAPULTA (tick de activación):
     *   - Solo acepta NIL. Transiciona a fase 2 (inicio del vuelo).
     *
     * FASE 2 — EN VUELO (avance automático):
     *   - Avanza una celda en la dirección de vuelo.
     *   - Colisión con borde/muro/portal sin llave → aterriza en celda anterior.
     *   - Colisión sobre agua → muerte (null).
     *   - Sin colisión: recoge recursos, detecta catapulta encadenada (fase 3)
     *     o sigue volando (fase 2).
     *
     * FASE 3 — ATERRIZAJE EN CATAPULTA DURANTE VUELO:
     *   - Solo acepta NIL. Transiciona a fase 2 con nueva dirección.
     *
     * @param n Nodo actual.
     * @param a Acción a aplicar.
     * @return Nuevo estado, o null si la acción es inválida o mortal.
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

            // Recoger moneda (si hay y no se tiene el máximo)
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }

            // Recoger llave
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);
                if (!l) l = true;
            }

            // Comprobar catapulta
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                if (!catapultasGratis && m <= 0) return null;  // Sin monedas
                if (!catapultasGratis) m--;
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Nodo(nx, ny, m, l, mB, lB, cB, 1, dir[0], dir[1]);
            }
            return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

        // --- FASE 1: Tick de activación de catapulta ---
        } else if (n.fase == 1) {
            if (a != ACTIONS.ACTION_NIL) return null;
            return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 2, n.vdx, n.vdy);

        // --- FASE 2: En vuelo ---
        } else if (n.fase == 2) {
            if (a != ACTIONS.ACTION_NIL) return null;
            int tx = n.x + n.vdx, ty = n.y + n.vdy;

            // Detectar colisión
            boolean col = (tx < 0 || tx >= gridW || ty < 0 || ty >= gridH);
            if (!col) col = (muro[tx][ty] && !agua[tx][ty]);
            if (!col && tx == metaX && ty == metaY && !n.llave) col = true;

            if (col) {
                if (agua[n.x][n.y]) return null;  // Muerte por agua
                return new Nodo(n.x, n.y, n.mon, n.llave, n.mB, n.lB, n.cB, 0, 0, 0);
            }

            int nx = tx, ny = ty;
            int m = n.mon; boolean l = n.llave; int mB = n.mB, lB = n.lB, cB = n.cB;

            // Recoger moneda en vuelo
            int mi = monIdx(nx, ny);
            if (mi >= 0 && (mB & (1 << mi)) != 0 && m < 5) { m++; mB &= ~(1 << mi); }

            // Recoger llave en vuelo
            int li = llaveIdx(nx, ny);
            if (li >= 0 && (lB & (1 << li)) != 0) {
                lB &= ~(1 << li);
                if (!l) l = true;
            }

            // Victoria en vuelo (portal con llave)
            if (nx == metaX && ny == metaY && l)
                return new Nodo(nx, ny, m, l, mB, lB, cB, 0, 0, 0);

            // Encadenamiento de catapultas
            long pk = enc(nx, ny);
            Integer ci = catIdx.get(pk);
            if (ci != null && (cB & (1 << ci)) != 0) {
                int[] dir = catDir.get(pk);
                cB &= ~(1 << ci);
                return new Nodo(nx, ny, m, l, mB, lB, cB, 3, dir[0], dir[1]);
            }

            // Seguir volando
            return new Nodo(nx, ny, m, l, mB, lB, cB, 2, n.vdx, n.vdy);

        // --- FASE 3: Rebote en catapulta durante vuelo ---
        } else if (n.fase == 3) {
            if (a != ACTIONS.ACTION_NIL) return null;
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
     * coordenadas dentro del mapa.
     */
    private long enc(int x, int y) { return (long)y * gridW + x; }

    /**
     * Devuelve el desplazamiento {dx, dy} correspondiente a una acción.
     * El eje Y crece hacia abajo (convenio GVGAI):
     * RIGHT=(+1,0), LEFT=(-1,0), UP=(0,-1), DOWN=(0,+1).
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
     * @return Índice 0..numMon-1, o -1 si no hay moneda.
     */
    private int monIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numMon; i++) if (monPos[i] == k) return i;
        return -1;
    }

    /**
     * Genera la clave única (String) de un nodo para indexar en tablaH
     * y detectar nodos repetidos. Serializa todos los campos relevantes
     * del nodo: posición, monedas, llave, bitmasks, fase y dirección de vuelo.
     *
     * @param n Nodo a serializar.
     * @return Clave del nodo como String "x,y,mon,llave,mB,lB,cB,fase,vdx,vdy".
     */
    private String keyNodo(Nodo n) {
        return n.x + "," + n.y + "," + n.mon + "," + (n.llave ? 1 : 0) + ","
                + n.mB + "," + n.lB + "," + n.cB + "," + n.fase + "," + n.vdx + "," + n.vdy;
    }

    /**
     * Busca el índice de la llave en la posición (x, y) para el bitmask lB.
     * @return Índice 0..numLlaves-1, o -1 si no hay llave.
     */
    private int llaveIdx(int x, int y) {
        long k = enc(x, y);
        for (int i = 0; i < numLlaves; i++) if (llavePos[i] == k) return i;
        return -1;
    }


    // =====================================================================
    //  CLASE INTERNA — Nodo
    // =====================================================================

    /**
     * Representa un estado completo del juego para la búsqueda RTA*.
     *
     * Codifica toda la información necesaria para distinguir dos situaciones
     * de juego diferentes: posición del agente, recursos (monedas portadas,
     * llave), recursos disponibles en el mapa (bitmasks) y fase de vuelo.
     *
     * CAMPOS:
     * - x, y:     Posición del agente en la cuadrícula (celdas).
     * - mon:      Número de monedas que porta el agente (0-5).
     * - llave:    true si el agente posee la llave.
     * - mB:       Bitmask de monedas aún disponibles en el mapa.
     * - lB:       Bitmask de llaves aún disponibles en el mapa.
     * - cB:       Bitmask de catapultas aún disponibles en el mapa.
     * - fase:     Fase de vuelo (0=normal, 1=catapulta, 2=vuelo, 3=rebote).
     * - vdx, vdy: Dirección de vuelo actual (solo relevante en fases 1-3).
     *
     * Los bitmasks se inicializan a (1<<n)-1, poniendo a 1 los n bits menos
     * significativos (todos los recursos disponibles). Al recoger/usar un
     * recurso, se pone su bit a 0 con la operación AND NOT: bitmask &= ~(1<<idx).
     */
    private static class Nodo {
        int x, y, mon, mB, lB, cB, fase, vdx, vdy;
        boolean llave;

        /**
         * Constructor del estado.
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
        Nodo(int x, int y, int m, boolean l, int mB, int lB, int cB,
               int f, int vx, int vy) {
            this.x = x; this.y = y; mon = m; llave = l;
            this.mB = mB; this.lB = lB; this.cB = cB;
            fase = f; vdx = vx; vdy = vy;
        }
    }
}