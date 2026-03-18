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
 * Búsqueda en profundidad iterativa con pila (Stack).
 * Reutiliza la misma infraestructura del A*:
 *   - Misma clave de estado (posición + monedas + recursos en mapa)
 *   - Misma agrupación de catapultas (pisar + vuelo NIL = una transición)
 *   - Misma reconstrucción de plan (nodos → acciones)
 *
 * Pseudocódigo DFS (transparencias pág. 14):
 *   estado[inicial] = VISITADO
 *   DFS_search(inicial, objetivo)
 *
 *   DFS_search(u, objetivo):
 *     if u == objetivo: return TRUE
 *     for each v in sucesores(u):
 *       if estado[v] == NOVISITADO:
 *         estado[v] = VISITADO
 *         padre[v] = u
 *         return DFS_search(v, objetivo)
 *     return FALSE
 *
 * Implementación iterativa equivalente con Stack y búsqueda
 * asíncrona (time-bounded) para no exceder el tiempo de GVGAI.
 */
public class AgenteProfundidad extends AbstractPlayer {

    private int blockSize;
    private int metaX, metaY;

    private ArrayList<ACTIONS> planDeAccion;

    // Estructuras de búsqueda (persistentes entre ticks)
    private Stack<Nodo> frontera;
    private HashMap<String, Integer> visitados;

    // Control
    private boolean buscando;
    private boolean metricasEnviadas;
    private int nodosExpandidos;
    private int profundidadMaxima;

    // =========================================================
    //  CONSTRUCTOR — solo inicialización, búsqueda en act()
    // =========================================================
    public AgenteProfundidad(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super();
        blockSize = stateObs.getBlockSize();

        ArrayList<Observation>[] portales = stateObs.getPortalsPositions();
        if (portales != null && portales.length > 0 && !portales[0].isEmpty()) {
            metaX = gridX(portales[0].get(0).position);
            metaY = gridY(portales[0].get(0).position);
        }

        planDeAccion = new ArrayList<>();
        frontera = new Stack<>();
        visitados = new HashMap<>();
        buscando = true;
        metricasEnviadas = false;
        nodosExpandidos = 0;
        profundidadMaxima = 0;

        // Nodo raíz en la frontera
        Nodo raiz = new Nodo(stateObs);
        frontera.push(raiz);

        System.out.println("AgenteDFS inicializado. Meta: (" + metaX + "," + metaY + ")");
    }

    // =========================================================
    //  ACT
    // =========================================================
    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Fase 1: búsqueda (el agente envía NIL mientras piensa)
        if (buscando) {
            buscarDFS(elapsedTimer);
        }

        // Fase 2: ejecución del plan
        if (!buscando && !planDeAccion.isEmpty()) {
            return planDeAccion.remove(0);
        }

        return ACTIONS.ACTION_NIL;
    }

    // =========================================================
    //  DFS ASÍNCRONO (time-bounded)
    // =========================================================
    private void buscarDFS(ElapsedCpuTimer timer) {
        Nodo nodoDestino = null;

        while (!frontera.isEmpty()) {
            // Control de tiempo: pausar si quedan menos de 5ms
            if (timer.remainingTimeMillis() < 5) {
                return; // Continuará en el siguiente tick
            }

            Nodo actual = frontera.pop();

            StateObservation estadoActual = actual.estado;

            // A) ¿Game over?
            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    nodoDestino = actual;
                    break;
                }
                continue; // Murió → podar rama
            }

            // B) Control de visitados
            String idEstado = stateKey(estadoActual);

            if (visitados.containsKey(idEstado) && visitados.get(idEstado) <= actual.coste) {
                continue; // Ya visitado con coste menor o igual → podar
            }
            visitados.put(idEstado, actual.coste);

            // Métricas
            nodosExpandidos++;
            if (actual.profundidad > profundidadMaxima) {
                profundidadMaxima = actual.profundidad;
            }

            // C) Expandir sucesores
            int avatarTypeActual = estadoActual.getAvatarType();
            int monedasActual = estadoActual.getAvatarResources().getOrDefault(15, 0);
            Vector2d posActual = estadoActual.getAvatarPosition();
            int gxAct = gridX(posActual), gyAct = gridY(posActual);

            ACTIONS[] dirs = {ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN,
                              ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT};

            for (ACTIONS dir : dirs) {
                StateObservation copia = estadoActual.copy();
                copia.advance(dir);

                // Muerto → descartar
                if (copia.isGameOver() &&
                    copia.getGameWinner() != ontology.Types.WINNER.PLAYER_WINS) {
                    continue;
                }

                ArrayList<ACTIONS> acciones = new ArrayList<>();
                acciones.add(dir);
                int costeExtra = 1;

                // ¿Catapulta activada? (tipo de avatar cambió + monedas bajaron)
                if (!copia.isGameOver()) {
                    int monedasDespues = copia.getAvatarResources().getOrDefault(15, 0);
                    int avatarTypeDespues = copia.getAvatarType();

                    boolean catapulta = (avatarTypeDespues != avatarTypeActual)
                        && (monedasDespues < monedasActual);

                    if (catapulta) {
                        // Simular vuelo: NILs hasta que avatar vuelva a tipo original
                        for (int t = 0; t < 30; t++) {
                            copia.advance(ACTIONS.ACTION_NIL);
                            acciones.add(ACTIONS.ACTION_NIL);
                            costeExtra++;

                            if (copia.isGameOver()) break;
                            if (copia.getAvatarType() == avatarTypeActual) break;
                        }
                    }
                }

                // Crear nodo hijo con la lista de acciones agrupada
                Nodo hijo = new Nodo(copia, actual, acciones, actual.coste + costeExtra);
                frontera.push(hijo);
            }
        }

        // Búsqueda terminada
        buscando = false;

        if (nodoDestino != null) {
            construirPlan(nodoDestino);
            System.out.println("DFS: Ruta encontrada. Acciones: " + planDeAccion.size());
        } else {
            System.out.println("DFS: No se encontró solución.");
        }

        // Métricas
        if (!metricasEnviadas) {
            MetricsProvider mp = MetricsProvider.getInstance();
            mp.setNodosExpandidos(nodosExpandidos);
            mp.setProfundidadMaxima(profundidadMaxima);
            mp.setNodosAbiertos(frontera.size());
            mp.setNodosCerrados(visitados.size());
            mp.setNumAccionesPlan(nodoDestino != null ? planDeAccion.size() : -1);
            mp.printMetrics();
            metricasEnviadas = true;
        }
    }

    // =========================================================
    //  RECONSTRUIR PLAN (nodos → acciones)
    // =========================================================
    private void construirPlan(Nodo nodoFinal) {
        Deque<Nodo> pila = new ArrayDeque<>();
        for (Nodo n = nodoFinal; n.padre != null; n = n.padre) {
            pila.push(n);
        }
        while (!pila.isEmpty()) {
            Nodo n = pila.pop();
            if (n.accionesDesdeParent != null) {
                planDeAccion.addAll(n.accionesDesdeParent);
            }
        }
    }

    // =========================================================
    //  CLAVE DE ESTADO — idéntica al A*
    // =========================================================
    private String stateKey(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        int gx = gridX(pos), gy = gridY(pos);
        int monedas = so.getAvatarResources().getOrDefault(15, 0);

        long resBits = 0;
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null) {
            for (ArrayList<Observation> lista : rec) {
                for (Observation obs : lista) {
                    int rx = gridX(obs.position);
                    int ry = gridY(obs.position);
                    resBits |= (1L << ((ry * 16 + rx) & 63));
                }
            }
        }
        return gx + "," + gy + "," + monedas + "," + resBits;
    }

    // =========================================================
    //  UTILIDADES
    // =========================================================
    private int gridX(Vector2d pos) { return (int)(pos.x / blockSize); }
    private int gridY(Vector2d pos) { return (int)(pos.y / blockSize); }
}