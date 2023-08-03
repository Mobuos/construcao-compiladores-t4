package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.LAParser.CmdChamadaContext;
import br.ufscar.dc.compiladores.LAParser.Exp_aritmeticaContext;
import br.ufscar.dc.compiladores.LAParser.Exp_relacionalContext;
import br.ufscar.dc.compiladores.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.LAParser.FatorContext;
import br.ufscar.dc.compiladores.LAParser.Fator_logicoContext;
import br.ufscar.dc.compiladores.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.LAParser.ParametroContext;
import br.ufscar.dc.compiladores.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_logicaContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_nao_unarioContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_unarioContext;
import br.ufscar.dc.compiladores.LAParser.RegistroContext;
import br.ufscar.dc.compiladores.LAParser.TermoContext;
import br.ufscar.dc.compiladores.LAParser.Termo_logicoContext;
import br.ufscar.dc.compiladores.LAParser.TipoContext;
import br.ufscar.dc.compiladores. LAParser.Tipo_basicoContext;
import br.ufscar.dc.compiladores.LAParser.Tipo_variavelContext;
import br.ufscar.dc.compiladores.LAParser.VariavelContext;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoDeclaracao;
public class LASemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();
    
    // Função auxiliar para adicionar erros semânticos.
    public static void adicionarErroSemantico
    (
        Token t,
        String mensagem
    ) 
    {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Função auxiliar para comparar tipos
    public static Boolean tiposCompativeis
    (
        TipoDeclaracao tipo1,
        TipoDeclaracao tipo2
    )
    {
        if (tipo1 == tipo2){
            return true;
        }
        // Verifica a compatibilidade entre REAL e INTEIROS.
        else if (
                (
                    tipo1 == TipoDeclaracao.REAL 
                    && tipo2 == TipoDeclaracao.INTEIRO
                )
                || 
                (
                    tipo1 == TipoDeclaracao.INTEIRO 
                    && tipo2 == TipoDeclaracao.REAL
                ) 
            ){
            return true;
        }
        // Verifica a compatibilidade entre PONTEIROS e ENDEREÇOS.
        else if (
            (tipo1 == TipoDeclaracao.PONTEIRO &&
            tipo2 == TipoDeclaracao.ENDERECO) ||
            (tipo1 == TipoDeclaracao.ENDERECO &&
            tipo2 == TipoDeclaracao.PONTEIRO
            ))
            {
                return true;
            }   
        return false;
    }

    // Função auxiliar para adicionar identificadores na tabela de dados
    // das funções/procedimentos. 
    public static void adicionarParametroNaTabela
    (
        Escopo escopo,
        TabelaDeSimbolos tabela,
        ParametroContext param,
        TipoDeclaracao tipoIdent
    )
    {
        param.identificador().forEach(ident -> {
            if (tabela.existe(ident.getText()) || tabela.existe(ident.IDENT(0).getText())){
                adicionarErroSemantico(
                    ident.start,
                    "identificador " + ident.IDENT(0).getText() + " ja declarado anteriormente"
                );
            }
            else{
                switch(tipoIdent){
                    case TIPO:
                        TabelaDeSimbolos dados = recuperarEstruturaTipo(param.tipo_variavel().IDENT().getText(), escopo);
                        tabela.adicionarRegistro(ident.IDENT(0).getText(), dados);
                        break;
                    default:
                        tabela.adicionar(ident.IDENT(0).getText(), tipoIdent);
                        break;
                }
            }
        });
    }

    // Função auxiliar para adicionar variáveis na tabela.
    public static void adicionarVariaveisNaTabela
    (
        Escopo escopo,
        TabelaDeSimbolos tabela,
        VariavelContext ctx
    )
    {
        TipoDeclaracao tipo = LASemanticoUtils.verificarTipo(escopo, ctx);

        if (tipo == TipoDeclaracao.INVALIDO){
            adicionarErroSemantico(ctx.tipo().start, "tipo " + ctx.tipo().getText() + " nao declarado" );
        }
        ctx.identificador().forEach(ident -> {
            if (tabela.existe(ident.getText()) || tabela.existe(ident.IDENT(0).getText())){
                adicionarErroSemantico(
                    ident.start,
                    "identificador " + ident.IDENT(0).getText() + " ja declarado anteriormente"
                );
            }
            else{
                switch(tipo){
                    case REGISTRO:
                        adicionarRegistroNoEscopo(escopo, ctx.tipo().registro(), ident.getText(), false);
                        break;
                    case TIPO:
                        TabelaDeSimbolos dados = recuperarEstruturaTipo(ctx.tipo().tipo_variavel().IDENT().getText(), escopo);
                        tabela.adicionarRegistro(ident.IDENT(0).getText(), dados);
                        break;
                    default:
                        tabela.adicionar(ident.IDENT(0).getText(), tipo);
                        break;
                }
            }
        });
    }

    // Função para adicionar um novo registro a tabela atual.
    public static void adicionarRegistroNoEscopo
    (
        Escopo escopo,
        RegistroContext ctx,
        String nome,
        Boolean isType
    )
    {
        TabelaDeSimbolos tabelaAtual = escopo.escopoAtual();
        TabelaDeSimbolos dados = new TabelaDeSimbolos();


        ctx.variavel().forEach(variavel -> {
            adicionarVariaveisNaTabela(escopo, dados, variavel);
        });

        if (isType){
            tabelaAtual.adicionarTipo(nome, dados);
        }
        else{
            tabelaAtual.adicionarRegistro(nome, dados);
        }
    }

    // Função para recuperar a estrutura de dados do registro.
    public static TabelaDeSimbolos recuperarEstruturaTipo
    (
        String nomeRegistro,
        Escopo escopo
    )
    {
        LinkedList<TabelaDeSimbolos> todosEscopos = escopo.recuperarTodosEscopos();

        for (TabelaDeSimbolos tabela: todosEscopos ){
            if (tabela.existe(nomeRegistro)){
                return tabela.recuperarRegistro(nomeRegistro);
            }
        }

        return null;
    }
    
     // Verifica se existe o identificador.
    public static Boolean existeIdentificadorTodosEscopos
    (
        Escopo escopo,
        IdentificadorContext ctx
    )
    {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.recuperarTodosEscopos();
        String nome = ctx.IDENT().get(0).getText();
        boolean existeVariavel = false;

        for (int i = 0; i < ctx.PONTO().size(); i++){
            nome += "." + ctx.IDENT(i+1);
        }

        for (TabelaDeSimbolos tabela: tabelas){
            if (tabela.existe(nome)){
                existeVariavel = true;
                break;
            }
        }

        return existeVariavel;
    }

    // Verifica tipo de identificador, varrendo todos os escopos.
    public static TipoDeclaracao getTipoDeTodosEscopos
    (
        Escopo escopo,
        String nome
    )
    {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.recuperarTodosEscopos();

        for (TabelaDeSimbolos tabela : tabelas){
            if (tabela.existe(nome)){
                return tabela.verificar(nome);
            }
        }

        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo básico.
    public static TipoDeclaracao verificarTipo
    (
        Tipo_basicoContext ctx)
    {
        if (ctx.LITERAL() != null){
            return TipoDeclaracao.LITERAL;
        }
        else if (ctx.INTEIRO() != null){
            return TipoDeclaracao.INTEIRO;
        }
        else if (ctx.LOGICO() != null){
            return TipoDeclaracao.LOGICO;
        }
        else if (ctx.REAL() != null){
            return TipoDeclaracao.REAL;
        }
        else {
            return TipoDeclaracao.INVALIDO;
        }
    }

    // Função de verificação do tipo de variável.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Tipo_variavelContext ctx
    )
    {
        // Caso haja o simbolo de ponteiro antes é declarado como ponteiro.
        if (ctx.PONTEIRO() != null){
            return TipoDeclaracao.PONTEIRO;
        }

        // Caso seja um identificador, é um registro,
        // então é necessário ver se o tipo de registro existe.
        else if (ctx.IDENT() != null) {
            List<TabelaDeSimbolos> tabelas = escopo.recuperarTodosEscopos();

            for (TabelaDeSimbolos tabela: tabelas){
                if (tabela.existe(ctx.IDENT().getText())){
                    return TipoDeclaracao.TIPO;
                }
            }
            return TipoDeclaracao.INVALIDO;
        }
        
        // É uma variável de tipo básico.
        else {
            return verificarTipo(ctx.tipo_basico());
        }
    }

    // Função auxiliar para o contexto "Tipo"
    // chamando a verificação do tipo de variável.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        TipoContext ctx
    )
    {
        if (ctx.tipo_variavel() != null){
            return verificarTipo(escopo, ctx.tipo_variavel());
        }
        else{
            return TipoDeclaracao.REGISTRO;
        }
    }

    // Verifica tipo de variável.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        VariavelContext ctx
    )
    {
        return verificarTipo(escopo, ctx.tipo());
    }

    // Verifica tipo de parcela unária.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Parcela_unarioContext ctx
    )
    {
        if (ctx.identificador() != null){
            String nome = ctx.identificador().IDENT(0).getText();

            if (ctx.identificador().PONTO() != null){
                for (int i = 0; i < ctx.identificador().PONTO().size(); i++){
                    nome += "." + ctx.identificador().IDENT(i+1);
                }
            }
            
            return getTipoDeTodosEscopos(escopo, nome);
        }

        if (ctx.PONTEIRO() != null){
            return TipoDeclaracao.PONTEIRO;
        }

        if (ctx.NUM_INT() != null){
            return TipoDeclaracao.INTEIRO;
        }

        if (ctx.NUM_REAL() != null){
            return TipoDeclaracao.REAL;
        }

        if (ctx.exp_unica != null){
            return verificarTipo(escopo, ctx.exp_unica);
        }

        if (ctx.cmdChamada() != null){
            return verificarTipo(escopo, ctx.cmdChamada());
        }

        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de cmdChamada
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        CmdChamadaContext ctx
    )
    {
        String nome = ctx.IDENT().getText();

        return getTipoDeTodosEscopos(escopo, nome);
    }

    // Verifica tipo de termo lógico.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Termo_logicoContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.fator_logico(0));
        
        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Fator_logicoContext fator_logico : ctx.fator_logico()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, fator_logico);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }
            // Se existe um operador lógico, e todos os valores dos termos lógicos
            // são compatíveis, retorna tipo valor como LÓGICO.
            if (ctx.op_logico_2().size() > 0){
                return TipoDeclaracao.LOGICO;
            }
        }

        return tipoAlvo;
    }

    // Verifica tipo de expressão.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        ExpressaoContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.termo_logico(0));

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Termo_logicoContext termo_logico : ctx.termo_logico()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, termo_logico);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }

            // Se existe um operador lógico, e todos os valores dos termos lógicos
            // são compatíveis, retorna tipo valor como LÓGICO.
            if (ctx.op_logico_1().size() > 0){
                return TipoDeclaracao.LOGICO;
            }
        }
        
        return tipoAlvo;
    }

    // Verifica tipo de fator lógico.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Fator_logicoContext ctx
    )
    {
        if (ctx.parcela_logica() != null){
            return LASemanticoUtils.verificarTipo(escopo, ctx.parcela_logica());
        }
        
        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de parcela lógica.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Parcela_logicaContext ctx
    )
    {
        if (ctx.TRUE() != null || ctx.FALSE() != null){
            return TipoDeclaracao.LOGICO;
        }
        
        return LASemanticoUtils.verificarTipo(escopo, ctx.exp_relacional());   
    }

    // Verifica tipo de expressão relacional.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Exp_relacionalContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.exp_aritmetica(0));

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Exp_aritmeticaContext exp_aritmetica : ctx.exp_aritmetica()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, exp_aritmetica);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }
            
            // Se existe um operador relaciona, e todos os valores dos termos lógicos
            // são compatíveis, retorna tipo valor como LÓGICO.
            if (ctx.op_relacional() != null){
                return TipoDeclaracao.LOGICO;
            }
        }

        return tipoAlvo;
    }

    // Verifica tipo de expressão aritmética.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Exp_aritmeticaContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.termo(0));

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (TermoContext termo : ctx.termo()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, termo);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }
        }

        return tipoAlvo;
    }

    // Verifica tipo do termo, operações de + e -.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        TermoContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.fator(0));
        
        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (FatorContext fator : ctx.fator()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, fator);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }
        }

        return tipoAlvo;
    }

    // Verifica tipo do fator, operações de * e /.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        FatorContext ctx
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(escopo, ctx.parcela(0));

        if (tipoAlvo == TipoDeclaracao.INVALIDO){
            for (ParcelaContext parcela : ctx.parcela()) {
                TipoDeclaracao tipoTestado = verificarTipo(escopo, parcela);
    
                if (!tiposCompativeis(tipoTestado, tipoAlvo)){
                    return TipoDeclaracao.INVALIDO;
                }
            }
        }

        return tipoAlvo;
    }

    // Verifica tipo de parcela, operações de %.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        ParcelaContext ctx
    )
    {
        if (ctx.parcela_unario() != null){
            return verificarTipo(escopo, ctx.parcela_unario());
        }
        else if (ctx.parcela_nao_unario() != null) {
            return verificarTipo(escopo, ctx.parcela_nao_unario());
        }
        
        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de parcela não unária.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        Parcela_nao_unarioContext ctx
    )
    {
        TipoDeclaracao tipoIdenficador = TipoDeclaracao.INVALIDO;

        if (ctx.identificador() != null){
            tipoIdenficador = LASemanticoUtils.verificarTipo(escopo, ctx.identificador());
        }

        if (ctx.CADEIA() != null){
            tipoIdenficador = TipoDeclaracao.LITERAL;
        }

        if (ctx.ENDERECO() != null){
            tipoIdenficador = TipoDeclaracao.ENDERECO;
        }
        
        return tipoIdenficador;
    }

    // Verifica tipo de acordo com o identificador.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        IdentificadorContext ctx
    )
    {
        
        if (ctx.IDENT() != null){   
            String nome = ctx.IDENT(0).getText();
    
            if (ctx.PONTO() != null){
                for (int i = 0; i < ctx.PONTO().size(); i++){
                    nome += "." + ctx.IDENT(i+1);
                }
            }
            return getTipoDeTodosEscopos(escopo, nome);
        }
        return TipoDeclaracao.INVALIDO;
    }
}
