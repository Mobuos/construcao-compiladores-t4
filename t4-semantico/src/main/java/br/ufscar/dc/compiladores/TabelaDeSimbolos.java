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
        PROCEDIMENTO,
        ENDERECO,
        TIPO,
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
    
    public TabelaDeSimbolos
    ()
    {
        this.tabela = new HashMap<>();
    }
    
    public void adicionar
    (
        String nome,
        TipoDeclaracao tipo
    )
    {
        tabela.put(nome, new EntradaTabelaDeSimbolos(tipo));
    }

    public void adicionarRegistro
    (
        String nome,
        TabelaDeSimbolos dadosRegistro
    )
    {
        tabela.put(nome, new EntradaTabelaDeSimbolos(TipoDeclaracao.REGISTRO, dadosRegistro));
    }

    public void adicionarTipo
    (
        String nome, 
        TabelaDeSimbolos dadosRegistro
    )
    {
        tabela.put(nome, new EntradaTabelaDeSimbolos(TipoDeclaracao.TIPO, dadosRegistro));
    }

    public void adicionar
    (
        String nome,
        TipoDeclaracao tipo,
        TabelaDeSimbolos dados
    )
    {
        tabela.put(nome, new EntradaTabelaDeSimbolos(tipo, dados));
    }
    
    public boolean existe
    (
        String nome
    )
    {
        if (!nome.contains(".")){
            return tabela.containsKey(nome);
        }
        else{
            String[] nomeSubString = nome.split("\\.");
            HashMap<String, EntradaTabelaDeSimbolos> tabelaAtual = tabela;

            for (String subString: nomeSubString){
                if (!tabelaAtual.containsKey(subString)){
                    return false;
                }
                if (tabelaAtual.get(subString).tipo == TipoDeclaracao.REGISTRO){
                    tabelaAtual = tabelaAtual.get(subString).dados.tabela;
                }
                else{
                    break;
                }
            }
            return true;
        }
    }
    
    public TipoDeclaracao verificar
    (
        String nome
    )
    {
        if (!nome.contains(".")){
            if (!tabela.containsKey(nome)){
                System.out.println("ERRO: Chave \""+ nome +"\" não encontrada na tabela!");
                return TipoDeclaracao.INVALIDO;
            }
            return tabela.get(nome).tipo;
        }
        else{
            String[] nomeSubString = nome.split("\\.");
            HashMap<String, EntradaTabelaDeSimbolos> tabelaAtual = tabela;
            
            for (String subString: nomeSubString){
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

    public TabelaDeSimbolos recuperarRegistro 
    (
        String nome
    )
    {
        return tabela.get(nome).dados;
    }

    public Integer tamanhoTabela
    ()
    {
        return tabela.size();
    }

    public TipoDeclaracao recuperarTipoParametro
    (
        Integer idx
    )
    {
        String keyOfIdx = tabela.keySet().toArray(new String[0])[idx];

        return tabela.get(keyOfIdx).tipo;
    }
}
