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
 * Búsqueda no informada offline con DFS iterativo y advance().
 * Cada tick = 1 nodo, coste 1. Sin agrupamiento.
 * Orden expansión: R, U, L, D, NIL.
 * Búsqueda asíncrona en act().
 */
public class AgenteProfundidad extends AbstractPlayer {

    private int blockSize;
    private int avatarTypeNormal;

    private ArrayList<ACTIONS> planDeAccion;
    private Stack<NodoDFS> frontera;
    private HashMap<String, Integer> visitados;

    private boolean buscando;
    private boolean metricasEnviadas;
    private int nodosExpandidos, profundidadMaxima;

    private static final ACTIONS[] ORDEN_EXPANSION = {
        ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_UP,
        ACTIONS.ACTION_LEFT,  ACTIONS.ACTION_DOWN,
        ACTIONS.ACTION_NIL
    };

    public AgenteProfundidad(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        super();
        blockSize = stateObs.getBlockSize();
        avatarTypeNormal = stateObs.getAvatarType();

        planDeAccion = new ArrayList<>();
        frontera = new Stack<>();
        visitados = new HashMap<>();
        buscando = true;
        metricasEnviadas = false;
        nodosExpandidos = 0;
        profundidadMaxima = 0;

        NodoDFS raiz = new NodoDFS(stateObs, null, ACTIONS.ACTION_NIL, 0, 0);
        frontera.push(raiz);
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        if (buscando) buscarDFS(elapsedTimer);
        if (!buscando && !planDeAccion.isEmpty()) return planDeAccion.remove(0);
        return ACTIONS.ACTION_NIL;
    }

    private void buscarDFS(ElapsedCpuTimer timer) {
        NodoDFS nodoDestino = null;

        while (!frontera.isEmpty()) {
            if (timer.remainingTimeMillis() < 3) return;

            NodoDFS actual = frontera.pop();
            StateObservation estadoActual = actual.so;

            if (estadoActual.isGameOver()) {
                if (estadoActual.getGameWinner() == ontology.Types.WINNER.PLAYER_WINS) {
                    nodoDestino = actual; break;
                }
                continue;
            }

            String idEstado = stateKey(estadoActual);
            if (visitados.containsKey(idEstado) && visitados.get(idEstado) <= actual.coste)
                continue;
            visitados.put(idEstado, actual.coste);

            nodosExpandidos++;
            if (actual.prof > profundidadMaxima) profundidadMaxima = actual.prof;

            // Push en orden inverso para que Stack saque R primero
            for (int i = ORDEN_EXPANSION.length - 1; i >= 0; i--) {
                ACTIONS accion = ORDEN_EXPANSION[i];
                StateObservation copia = estadoActual.copy();
                copia.advance(accion);

                if (copia.isGameOver() &&
                    copia.getGameWinner() != ontology.Types.WINNER.PLAYER_WINS)
                    continue;

                NodoDFS hijo = new NodoDFS(copia, actual, accion,
                    actual.coste + 1, actual.prof + 1);
                frontera.push(hijo);
            }
        }

        buscando = false;

        if (nodoDestino != null) {
            Deque<ACTIONS> pila = new ArrayDeque<>();
            for (NodoDFS n = nodoDestino; n.padre != null; n = n.padre)
                pila.push(n.accion);
            while (!pila.isEmpty()) planDeAccion.add(pila.pop());
        }

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

    private String stateKey(StateObservation so) {
        Vector2d pos = so.getAvatarPosition();
        int gx = gridX(pos), gy = gridY(pos);
        int monedas = so.getAvatarResources().getOrDefault(15, 0);
        int tipo = so.getAvatarType();

        long resBits = 0;
        ArrayList<Observation>[] rec = so.getResourcesPositions();
        if (rec != null)
            for (ArrayList<Observation> lista : rec)
                for (Observation obs : lista) {
                    int rx = gridX(obs.position), ry = gridY(obs.position);
                    resBits |= (1L << ((ry * 16 + rx) & 63));
                }

        long catBits = 0;
        ArrayList<Observation>[] inmov = so.getImmovablePositions();
        if (inmov != null)
            for (ArrayList<Observation> lista : inmov)
                for (Observation obs : lista)
                    if (obs.itype == 5) {
                        int cx = gridX(obs.position), cy = gridY(obs.position);
                        catBits |= (1L << ((cy * 16 + cx) & 63));
                    }

        if (tipo != avatarTypeNormal)
            return gx + "," + gy + "," + monedas + "," + tipo + ","
                + resBits + "," + catBits + ",t" + so.getGameTick();

        return gx + "," + gy + "," + monedas + "," + tipo + ","
            + resBits + "," + catBits;
    }

    private int gridX(Vector2d pos) { return (int)(pos.x / blockSize); }
    private int gridY(Vector2d pos) { return (int)(pos.y / blockSize); }

    private static class NodoDFS {
        StateObservation so; NodoDFS padre; ACTIONS accion; int coste, prof;
        NodoDFS(StateObservation so, NodoDFS padre, ACTIONS accion, int coste, int prof) {
            this.so = so; this.padre = padre; this.accion = accion;
            this.coste = coste; this.prof = prof;
        }
    }
}