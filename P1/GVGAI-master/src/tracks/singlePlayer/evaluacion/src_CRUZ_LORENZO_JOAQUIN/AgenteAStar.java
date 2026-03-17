package tracks.singlePlayer.evaluacion.src_CRUZ_LORENZO_JOAQUIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Stack;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tracks.singlePlayer.MetricsProvider;

/**
 * Agente basado en Búsqueda A* (A-Estrella Ponderado).
 * Implementa "Poda Maestra +1" para tolerar retardos de físicas (1 tick)
 * bloqueando matemáticamente los bucles espaciales.
 */
public class AgenteAStar extends AbstractPlayer {

    private ArrayList<ACTIONS> planDeAccion;
    private PriorityQueue<Nodo> frontera;
    private HashMap<String, Double> visitados; 
    
    private boolean buscando;
    private boolean metricasEnviadas;
    
    private int nodosExpandidos;
    private int profundidadMaxima;

    private class Nodo implements Comparable<Nodo> {
        StateObservation estado;
        Nodo padre;
        ACTIONS accion;
        double coste;      // g(n)
        double heuristica; // h(n)
        int profundidad;

        public Nodo(StateObservation estado, Nodo padre, ACTIONS accion, double coste, double heuristica, int profundidad) {
            this.estado = estado;
            this.padre = padre;
            this.accion = accion;
            this.coste = coste;
            this.heuristica = heuristica;
            this.profundidad = profundidad;
        }

        @Override
        public int compareTo(Nodo otro) {
            double miF = this.coste + this.heuristica;
            double otroF = otro.coste + otro.heuristica;
            return Double.compare(miF, otroF);
        }
    }

    public AgenteAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super();
        planDeAccion = new ArrayList<>();
        frontera = new PriorityQueue<>();
        visitados = new HashMap<>();
        
        buscando = true;
        metricasEnviadas = false;
        nodosExpandidos = 0;
        profundidadMaxima = 0;

        double hInicial = calcularHeuristica(stateObs);
        Nodo raiz = new Nodo(stateObs, null, ACTIONS.ACTION_NIL, 0, hInicial, 0);
        frontera.add(raiz);
        
        System.out.println("Agente A* instanciado. Listo para búsqueda asíncrona experta...");
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        if (buscando) {
            buscarRutaAsincrona(elapsedTimer);
        }

        if (!buscando && planDeAccion != null && !planDeAccion.isEmpty()) {
            return planDeAccion.remove(0);
        }
        
        return ACTIONS.ACTION_NIL; 
    }

    private void buscarRutaAsincrona(ElapsedCpuTimer elapsedTimer) {
        Nodo nodoDestino = null;

        while (!frontera.isEmpty()) {
            
            if (frontera.size() > 8000 || nodosExpandidos > 30000) {
                System.out.println("¡Alerta! Límite de seguridad alcanzado. Abortando...");
                break;
            }

            if (elapsedTimer.remainingTimeMillis() < 5) return; 

            Nodo actual = frontera.poll();
            nodosExpandidos++;
            
            if (actual.profundidad > profundidadMaxima) {
                profundidadMaxima = actual.profundidad;
            }

            StateObservation estadoActual = actual.estado;

            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    nodoDestino = actual; 
                    break; 
                } else {
                    actual.estado = null; 
                    continue; 
                }
            }

            boolean isFlying = (estadoActual.getAvatarPosition() == null);
            ArrayList<ACTIONS> accionesPosibles = estadoActual.getAvailableActions();
            if (isFlying) {
                accionesPosibles = new ArrayList<>();
                accionesPosibles.add(ACTIONS.ACTION_NIL);
            }

            for (ACTIONS accion : accionesPosibles) {
                StateObservation estadoHijo = estadoActual.copy();
                estadoHijo.advance(accion);

                double nuevoCoste = actual.coste + 1;
                String idHijo = generarIdEstado(estadoHijo); 
                boolean hijoFlying = (estadoHijo.getAvatarPosition() == null);

                if (!hijoFlying) {
                    double costeAnterior = visitados.containsKey(idHijo) ? visitados.get(idHijo) : Double.MAX_VALUE;
                    
                    // LA PODA MAESTRA DE +1:
                    // Permite esperar 1 tick para físicas (ej: pulsar USE en la catapulta).
                    // Pero prohíbe volver atrás (porque A -> B -> A cuesta 2 ticks y será podado).
                    if (nuevoCoste > costeAnterior + 1) {
                        continue; 
                    }
                    
                    // Solo guardamos el récord en la memoria si es estrictamente mejor
                    if (nuevoCoste < costeAnterior) {
                        visitados.put(idHijo, nuevoCoste);
                    }
                }
                
                double nuevaHeuristica = calcularHeuristica(estadoHijo);
                Nodo hijo = new Nodo(estadoHijo, actual, accion, nuevoCoste, nuevaHeuristica, actual.profundidad + 1);
                frontera.add(hijo);
            }
            
            actual.estado = null; 
        }
        
        buscando = false; 

        if (nodoDestino != null) {
            construirPlan(nodoDestino);
            System.out.println("¡Ruta A* encontrada! Pasos a dar: " + planDeAccion.size());
        } else {
            System.out.println("Búsqueda A* agotada o abortada por seguridad.");
        }

        if (!metricasEnviadas) {
            MetricsProvider metrics = MetricsProvider.getInstance();
            metrics.setNodosExpandidos(nodosExpandidos);
            metrics.setProfundidadMaxima(profundidadMaxima);
            metrics.setNodosAbiertos(frontera.size());
            metrics.setNodosCerrados(visitados.size());
            metrics.setNumAccionesPlan(nodoDestino != null ? planDeAccion.size() : -1);
            metrics.printMetrics();
            metricasEnviadas = true;
        }
    }

    /**
     * HEURÍSTICA PONDERADA (A* Agresivo x10).
     */
    private double calcularHeuristica(StateObservation estado) {
        Vector2d posAvatar = estado.getAvatarPosition();
        if (posAvatar == null) return 0; // Atajo absoluto: volar es bueno

        double distanciaMinima = 10000;
        double blockSize = estado.getBlockSize();

        ArrayList<Observation>[] portales = estado.getPortalsPositions();
        if (portales != null) {
            for (ArrayList<Observation> lista : portales) {
                for (Observation obs : lista) {
                    double dist = Math.abs(posAvatar.x - obs.position.x) + Math.abs(posAvatar.y - obs.position.y);
                    if (dist < distanciaMinima) distanciaMinima = dist;
                }
            }
        }
        if (distanciaMinima != 10000) return (distanciaMinima / blockSize) * 10.0;

        ArrayList<Observation>[] recursos = estado.getResourcesPositions();
        if (recursos != null) {
            for (ArrayList<Observation> lista : recursos) {
                for (Observation obs : lista) {
                    double dist = Math.abs(posAvatar.x - obs.position.x) + Math.abs(posAvatar.y - obs.position.y);
                    if (dist < distanciaMinima) distanciaMinima = dist;
                }
            }
        }
        if (distanciaMinima != 10000) return (distanciaMinima / blockSize) * 10.0;

        return 0; 
    }

    /**
     * Identificador Puro de Estado (Sin acciones pasadas).
     */
    private String generarIdEstado(StateObservation estado) {
        Vector2d pos = estado.getAvatarPosition();
        if (pos == null) return "volando_catapulta"; 
        
        StringBuilder id = new StringBuilder();
        id.append(pos.x).append("_").append(pos.y).append("_");
        
        Vector2d ori = estado.getAvatarOrientation();
        if (ori != null) id.append(ori.x).append("_").append(ori.y).append("_");
        
        id.append(estado.getAvatarType()).append("_");
        
        appendObs(id, estado.getImmovablePositions());
        appendObs(id, estado.getMovablePositions());
        appendObs(id, estado.getResourcesPositions());
        appendObs(id, estado.getNPCPositions());
        appendObs(id, estado.getPortalsPositions());
        appendObs(id, estado.getFromAvatarSpritesPositions()); 
        
        if (estado.getAvatarResources() != null) {
            for (Integer key : estado.getAvatarResources().keySet()) {
                id.append(key).append("=").append(estado.getAvatarResources().get(key)).append("_");
            }
        }
        return id.toString();
    }

    private void appendObs(StringBuilder sb, ArrayList<Observation>[] obsArrays) {
        if (obsArrays != null) {
            for (ArrayList<Observation> list : obsArrays) {
                for (Observation obs : list) {
                    sb.append(obs.itype).append("-").append(obs.position.x).append("-").append(obs.position.y).append("_");
                }
            }
        }
    }

    private void construirPlan(Nodo nodoFinal) {
        Nodo actual = nodoFinal;
        Stack<ACTIONS> pilaAcciones = new Stack<>();
        while (actual.padre != null) {
            pilaAcciones.push(actual.accion);
            actual = actual.padre;
        }
        while (!pilaAcciones.isEmpty()) {
            planDeAccion.add(pilaAcciones.pop());
        }
    }
}