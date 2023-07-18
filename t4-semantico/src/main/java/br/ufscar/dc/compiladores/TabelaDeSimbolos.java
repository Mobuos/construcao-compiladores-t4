package br.ufscar.dc.compiladores;

import java.util.HashMap;

public class TabelaDeSimbolos {
    public enum TipoDeclaracao {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        REGISTRO,
        PONTEIRO,
        FUNCAO,
        PROCEDIMENTO,
        ENDERECO,
        INVALIDO
    }
    
    private class EntradaTabelaDeSimbolos {
        TipoDeclaracao tipo;
        TabelaDeSimbolos dados = null;        

        private EntradaTabelaDeSimbolos(TipoDeclaracao tipo) {
            this.tipo = tipo;
            this.dados = null;
        }

        private EntradaTabelaDeSimbolos(TipoDeclaracao tipo, TabelaDeSimbolos dados){
            this.tipo = tipo;
            this.dados = dados;
        }
    }
    
    private final HashMap<String, EntradaTabelaDeSimbolos> tabela;
    
    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }
    
    public void adicionar(String nome, TipoDeclaracao tipo) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(tipo));
    }

    public void adicionarRegistro(String nome, TabelaDeSimbolos dadosRegistro){
        tabela.put(nome, new EntradaTabelaDeSimbolos(TipoDeclaracao.REGISTRO, dadosRegistro));
    }

    public void adicionar(String nome, TipoDeclaracao tipo, TabelaDeSimbolos dados){
        tabela.put(nome, new EntradaTabelaDeSimbolos(tipo, dados));
    }
    
    public boolean existe(String nome) {
        if (!nome.contains(".")){
            return tabela.containsKey(nome);
        }
        else{
            String[] nomeSubString = nome.split(".");
            HashMap<String, EntradaTabelaDeSimbolos> tabelaAtual = tabela;

            for (String subString: nomeSubString){
                if (!tabelaAtual.containsKey(subString)){
                    return false;
                }
                tabelaAtual = tabelaAtual.get(subString).dados.tabela;
            }
            return true;
        }
    }
    
    public TipoDeclaracao verificar(String nome) {
        if (!nome.contains(".")){
            return tabela.get(nome).tipo;
        }
        else{
            String[] nomeSubString = nome.split(".");
            HashMap<String, EntradaTabelaDeSimbolos> tabelaAtual = tabela;

            for (String subString: nomeSubString){
                LASemanticoUtils.adicionarErroSemantico(subString);
                LASemanticoUtils.adicionarErroSemantico(tabelaAtual.get(subString).tipo.toString());
                
                if (!tabelaAtual.containsKey(subString)){
                    break;
                }
                if (tabelaAtual.get(subString).tipo == TipoDeclaracao.REGISTRO){
                    tabelaAtual = tabelaAtual.get(subString).dados.tabela;
                }
                else{
                    return tabelaAtual.get(subString).tipo;
                }
            }

            return TipoDeclaracao.INVALIDO;
        }
    }   

    public TabelaDeSimbolos recuperarRegistro (String nome){
        return tabela.get(nome).dados;
    }
}
