package com.deckofcards;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;

/*
 * Queria ter melhorado a organização do código mas tive que fazer um trabalho da faculdade em paralelo
 * Autor: Rafael Dalli Soares
 */
public class App {

    public static void main(String[] args) {
        String deckId = criarNovoBaralho();

        if (deckId != null) {
            JSONObject alanHand = distribuirCartas(deckId, "Alan", 11);
            JSONObject brunoHand = distribuirCartas(deckId, "Bruno", 11);

            organizarCartas(alanHand);
            organizarCartas(brunoHand);

            boolean alanTemSequencia = temSequenciaDeTresConsecutivasDoMesmoNaipe(alanHand.getJSONArray("cartas"));
            boolean brunoTemSequencia = temSequenciaDeTresConsecutivasDoMesmoNaipe(brunoHand.getJSONArray("cartas"));

            formatarSaida(alanHand);
            formatarSaida(brunoHand);

            String vencedor = determinarVencedor(alanTemSequencia, brunoTemSequencia);

            JSONObject resultado = new JSONObject();
            resultado.put("alan", alanHand);
            resultado.put("bruno", brunoHand);
            resultado.put("vencedor", vencedor);

            System.out.println(resultado.toString(2));
        }
    }

    /*
     * Gera novo baralho ramdomizado da DeckOfCardsAPI 
     * Retorna o id do deck criado
     */
    public static String criarNovoBaralho() {
        String deckId = null;
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1")).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonObject = new JSONObject(response.body());
                deckId = jsonObject.getString("deck_id");
                System.out.println("Baralho criado com deck_id: " + deckId);
            } else {
                System.err.println("Erro ao criar o baralho.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return deckId;
    }

    /*
     * Faz o processo de destribuição de cartas dado o deck criado com o id do método acima
     * Retorna as cartas do jogador
     */
    public static JSONObject distribuirCartas(String deckId, String jogador, int quantidade) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://deckofcardsapi.com/api/deck/" + deckId + "/draw/?count=" + quantidade)).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonObject = new JSONObject(response.body());
                JSONArray cartas = jsonObject.getJSONArray("cards");

                for(int i = 0; i < cartas.length(); i++){
                    formataCarta(cartas.getJSONObject(i)); //Formata os valores do ás, valete, dama e rei
                    traduzirNaipe(cartas.getJSONObject(i)); //Traduz o nome dos naipes 
                }

                JSONObject jogadorHand = new JSONObject();
                jogadorHand.put("cartas", cartas);

                return jogadorHand;
            } else {
                System.err.println("Erro ao distribuir cartas para " + jogador);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * Separa as cartas dos jogadores pelos naipes com ajuda de HashMap para agrupamento
     */
    public static void organizarCartas(JSONObject jogadorHand) {
        JSONArray cartas = jogadorHand.getJSONArray("cartas");
        HashMap<String, JSONArray> naipes = new HashMap<>(); //HashMap para ajudar a organizar as cartas em naipes

        for (int i = 0; i < cartas.length(); i++) {
            JSONObject carta = cartas.getJSONObject(i);
            String naipe = carta.getString("suit");
            
            if (!naipes.containsKey(naipe)) { //Verifica se o HashMap já possui uma entrada para o naipe atual da carta
                naipes.put(naipe, new JSONArray());
            }
            naipes.get(naipe).put(carta);
        }

        JSONArray cartasOrganizadas = new JSONArray();
        for (String naipe : new String[]{"paus", "ouros", "copas", "espadas"}) {
            if (naipes.containsKey(naipe)) {
                sort(naipes.get(naipe)); //Ordena os valores de forma crescente
                cartasOrganizadas.put(naipes.get(naipe));
            }
        }

        jogadorHand.put("cartas", cartasOrganizadas);
    }
    
    public static void formatarSaida(JSONObject jogadorHand) {
        JSONArray cartas = jogadorHand.getJSONArray("cartas");
        HashMap<String, JSONArray> naipes = new HashMap<>();

        for (int i = 0; i < cartas.length(); i++) {
            for(int j = 0; j < cartas.getJSONArray(i).length(); j++){
                JSONObject carta = cartas.getJSONArray(i).getJSONObject(j);
                String naipe = carta.getString("suit");
                
                if (!naipes.containsKey(naipe)) {
                    naipes.put(naipe, new JSONArray());
                }
                naipes.get(naipe).put(carta.getString("value") + " de " + naipe);
            }
        }

        JSONArray cartasFormatadas = new JSONArray();
        for (String naipe : new String[]{"paus", "ouros", "copas", "espadas"}) {
            if (naipes.containsKey(naipe)) {
                cartasFormatadas.put(naipes.get(naipe));
            }
        }

        jogadorHand.put("cartas", cartasFormatadas);
    }

    /*
     * Método para verificar se existe sequencia consecutiva nas cartas dos jogadores
     * Retorna true ou false dependendo do resultado da comparação feita
     */
    public static boolean temSequenciaDeTresConsecutivasDoMesmoNaipe(JSONArray cartas) {
        int contador = 0;

        for (int i = 0; i < cartas.length(); i++) {
            if(cartas.getJSONArray(i).length() > 1)  //Se o JSONArray for < 1, então não executa os próximos passos
                for(int j = 1; j < cartas.getJSONArray(i).length(); j++){
                    if(Integer.parseInt(cartas.getJSONArray(i).getJSONObject(j).getString("value")) == Integer.parseInt(cartas.getJSONArray(i).getJSONObject(j - 1).getString("value")) + 1){
                        contador++;
                        if (contador == 3) {
                            return true; // Encontrou uma sequência de 3 cartas do mesmo naipe
                        }
                    }
                }    
        }

        return false; // Não encontrou uma sequência de 3 cartas do mesmo naipe
    }

    /*
     * Basicamente um Selection Sort
     */
    public static void sort(JSONArray cartas){

        for(int i = 0; i < cartas.length() - 1; i++){
            int min_idx = i;
            for (int j = i + 1; j < cartas.length(); j++){
                if (cartas.getJSONObject(j).getInt("value") < cartas.getJSONObject(min_idx).getInt("value"))
                    min_idx = j;
            }

            // Troca o elemento mínimo encontrado pelo primeiro elemento
            JSONObject temp = cartas.getJSONObject(i);
            cartas.put(i, cartas.getJSONObject(min_idx));
            cartas.put(min_idx, temp);
        }
    }

    public static void traduzirNaipe(JSONObject cartas) {
        switch (cartas.getString("suit")) {
            case "HEARTS":
                cartas.put("suit", "copas");
                break;
            case "DIAMONDS":
                cartas.put("suit", "ouros");
                break;
            case "CLUBS":
                cartas.put("suit", "paus");
                break;
            case "SPADES":
                cartas.put("suit", "espadas");
                break;
        }
    }

    public static void formataCarta(JSONObject cartas) {
        switch (cartas.getString("value")) {
            case "ACE":
                cartas.put("value", "1");
                break;
            case "JACK":
                cartas.put("value", "11");
                break;
            case "QUEEN":
                cartas.put("value", "12");
                break;
            case "KING":
                cartas.put("value", "13");
                break;
            default:
                cartas.put("value", cartas.get("value"));
                break;
        }
    }

    public static String determinarVencedor(boolean alanTemSequencia, boolean brunoTemSequencia) {
        if (alanTemSequencia && brunoTemSequencia) {
            return "Empate";
        } else if (alanTemSequencia) {
            return "Alan";
        } else if (brunoTemSequencia) {
            return "Bruno";
        } else {
            return "Nenhum jogador tem sequência";
        }
    }
}

