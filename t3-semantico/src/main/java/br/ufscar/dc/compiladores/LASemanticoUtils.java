package br.ufscar.dc.compiladores;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import br.ufscar.dc.compiladores.LAParser.Exp_aritmeticaContext;
import br.ufscar.dc.compiladores.LAParser.Exp_relacionalContext;
import br.ufscar.dc.compiladores.LAParser.ExpressaoContext;
import br.ufscar.dc.compiladores.LAParser.FatorContext;
import br.ufscar.dc.compiladores.LAParser.Fator_logicoContext;
import br.ufscar.dc.compiladores.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.LAParser.ParcelaContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_logicaContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_nao_unarioContext;
import br.ufscar.dc.compiladores.LAParser.Parcela_unarioContext;
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

        return false;
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
        TipoDeclaracao tipo;

        // Caso haja o simbolo de ponteiro antes é declarado como ponteiro.
        if (ctx.PONTEIRO() != null){
            return TipoDeclaracao.PONTEIRO;
        }

        // Caso seja um identificador, é um registro,
        // então é necessário ver se o tipo de registro existe.
        else if (ctx.IDENT() != null) {
            TabelaDeSimbolos tabela = escopo.escopoAtual();

            if (!tabela.existe(ctx.IDENT().getText())){
                return TipoDeclaracao.INVALIDO;
            }
            else{
                tipo = TipoDeclaracao.REGISTRO;
            }
        }
        
        // É uma variável de tipo básico.
        else {
            tipo = verificarTipo( ctx.tipo_basico());
        }

        
        return tipo;
    }

    // Função auxiliar para o contexto "Tipo"
    // chamando a verificação do tipo de variável.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        TipoContext ctx
    )
    {
        return verificarTipo(escopo, ctx.tipo_variavel());
    }

    // Verifica tipo de variável.
    // caso a variável não foi declarada,
    // adiciona na tabela de simbolos.
    public static TipoDeclaracao verificarTipo
    (
        Escopo escopo,
        VariavelContext ctx
    )
    {
        TipoDeclaracao tipo = verificarTipo(escopo, ctx.tipo());
        TabelaDeSimbolos tabela = escopo.escopoAtual();

        ctx.identificador().forEach(ident -> {
            if (tabela.existe(ident.getText())){
                adicionarErroSemantico(
                    ident.start,
                    "identificador " + ident.getText() + " ja declarado anteriormente"
                    );
            }
            else{
                tabela.adicionar(ident.getText(), tipo);
            }
        });

        if (tipo == TipoDeclaracao.INVALIDO){
            adicionarErroSemantico(ctx.tipo().start, "tipo " + ctx.tipo().getText() + " nao declarado" );
        }

        return tipo;
    }

    // Verifica se existe o identificador.
    public static Boolean existeIdentificador
    (
        IdentificadorContext ctx,
        Escopo escopo
    )
    {
        LinkedList<TabelaDeSimbolos> tabelas = escopo.recuperarTodosEscopos();
        String nome = ctx.IDENT().get(0).getText();
        boolean existeVariavel = false;

        for ( TabelaDeSimbolos tabela: tabelas){
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

    // Verifica tipo de parcela unária.
    public static TipoDeclaracao verificarTipo
    (
        Parcela_unarioContext ctx,
        Escopo escopo
    )
    {
        if (ctx.identificador() != null){
            String nome = ctx.identificador().IDENT(0).getText();
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

        if (ctx.CADEIA() != null){
            return TipoDeclaracao.LITERAL;
        }

        if (ctx.exp_unica != null){
            return verificarTipo(ctx.exp_unica, escopo);
        }

        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de termo lógico.
    public static TipoDeclaracao verificarTipo
    (
        Termo_logicoContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.fator_logico(0), escopo);
        
        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Fator_logicoContext fator_logico : ctx.fator_logico()) {
                TipoDeclaracao tipoTestado = verificarTipo(fator_logico, escopo);
    
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
        ExpressaoContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.termo_logico(0), escopo);

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Termo_logicoContext termo_logico : ctx.termo_logico()) {
                TipoDeclaracao tipoTestado = verificarTipo(termo_logico, escopo);
    
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
        Fator_logicoContext ctx,
        Escopo escopo
    )
    {
        if (ctx.parcela_logica() != null){
            return LASemanticoUtils.verificarTipo(ctx.parcela_logica(), escopo);
        }
        
        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de parcela lógica.
    public static TipoDeclaracao verificarTipo
    (
        Parcela_logicaContext ctx,
        Escopo escopo
    )
    {
        if (ctx.TRUE() != null || ctx.FALSE() != null){
            return TipoDeclaracao.LOGICO;
        }
        
        return LASemanticoUtils.verificarTipo(ctx.exp_relacional(), escopo);   
    }

    // Verifica tipo de expressão relacional.
    public static TipoDeclaracao verificarTipo
    (
        Exp_relacionalContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.exp_aritmetica(0), escopo);

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (Exp_aritmeticaContext exp_aritmetica : ctx.exp_aritmetica()) {
                TipoDeclaracao tipoTestado = verificarTipo(exp_aritmetica, escopo);
    
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
        Exp_aritmeticaContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.termo(0), escopo);

        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (TermoContext termo : ctx.termo()) {
                TipoDeclaracao tipoTestado = verificarTipo(termo, escopo);
    
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
        TermoContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.fator(0), escopo);
        
        if (tipoAlvo != TipoDeclaracao.INVALIDO){
            for (FatorContext fator : ctx.fator()) {
                TipoDeclaracao tipoTestado = verificarTipo(fator, escopo);
    
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
        FatorContext ctx,
        Escopo escopo
    )
    {
        TipoDeclaracao tipoAlvo = verificarTipo(ctx.parcela(0), escopo);

        if (tipoAlvo == TipoDeclaracao.INVALIDO){
            for (ParcelaContext parcela : ctx.parcela()) {
                TipoDeclaracao tipoTestado = verificarTipo(parcela, escopo);
    
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
        ParcelaContext ctx,
        Escopo escopo
    )
    {
        if (ctx.parcela_unario() != null){
            return verificarTipo(ctx.parcela_unario(), escopo);
        }
        else if (ctx.parcela_nao_unario() != null) {
            return verificarTipo(ctx.parcela_nao_unario(), escopo);
        }
        
        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de parcela não unária.
    public static TipoDeclaracao verificarTipo
    (
        Parcela_nao_unarioContext ctx,
        Escopo escopo
    )
    {
        if (ctx.identificador() != null){
            return LASemanticoUtils.verificarTipo(ctx.identificador(), escopo);
        }
        
        return TipoDeclaracao.INVALIDO;
    }

    // Verifica tipo de acordo com o identificador.
    public static TipoDeclaracao verificarTipo
    (
        IdentificadorContext ctx,
        Escopo escopo
    )
    {
        if (ctx.IDENT(0) != null){   
            return getTipoDeTodosEscopos(escopo, ctx.IDENT(0).getText());
        }
        return TipoDeclaracao.INVALIDO;
    }
}
