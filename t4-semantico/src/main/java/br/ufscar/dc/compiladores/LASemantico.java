package br.ufscar.dc.compiladores;

import br.ufscar.dc.compiladores.LAParser.CmdAtribuicaoContext;
import br.ufscar.dc.compiladores.LAParser.CmdChamadaContext;
import br.ufscar.dc.compiladores.LAParser.CmdContext;
import br.ufscar.dc.compiladores.LAParser.CorpoContext;
import br.ufscar.dc.compiladores.LAParser.Declaracao_funcoesContext;
import br.ufscar.dc.compiladores.LAParser.Declaracao_variaveisContext;
import br.ufscar.dc.compiladores.LAParser.DeclaracoesContext;
import br.ufscar.dc.compiladores.LAParser.IdentificadorContext;
import br.ufscar.dc.compiladores.LAParser.ParametroContext;
import br.ufscar.dc.compiladores.LAParser.RegistroContext;
import br.ufscar.dc.compiladores.TabelaDeSimbolos.TipoDeclaracao;

public class LASemantico extends LABaseVisitor<Void> {

    Escopo escopo = new Escopo();

    @Override
    public Void visitDeclaracoes
    (
        DeclaracoesContext ctx
    ) 
    {
        escopo.criarNovoEscopo();
        return super.visitDeclaracoes(ctx);
    }

    @Override
    public Void visitCorpo
    (
        CorpoContext ctx
    )
    {
        for (CmdContext cmd: ctx.cmd()){
            if (cmd.cmdRetorne() != null){
                LASemanticoUtils.adicionarErroSemantico(cmd.start, "comando retorne nao permitido nesse escopo");
            }
        }

        return super.visitCorpo(ctx);
    }

    @Override
    public Void visitDeclaracao_funcoes
    (
        Declaracao_funcoesContext ctx
    )
    {
        TabelaDeSimbolos tabelaForaFuncao = escopo.escopoAtual();
        String nome = ctx.IDENT().getText();
        TabelaDeSimbolos dadosParametros = new TabelaDeSimbolos();
        TipoDeclaracao tipoDeclarado = TipoDeclaracao.PROCEDIMENTO;
        
        if (ctx.FUNCAO() != null){
            tipoDeclarado = LASemanticoUtils.verificarTipo(escopo, ctx.tipo_variavel());
        }
        
        // Adiciona uma nova tabela no escopo para a função/procedimento criado.
        escopo.criarNovoEscopo();
        TabelaDeSimbolos tabelaDentroFuncao = escopo.escopoAtual();
        
        if (tabelaForaFuncao.existe(nome)){
            LASemanticoUtils.adicionarErroSemantico(ctx.start, "ja declarado");
        }
        
        // Caso exista parâmetros, adiciona os parâmetros a tabela de parâmetros.
        if (ctx.parametros() != null){
            for (ParametroContext parametro: ctx.parametros().parametro()){
                TipoDeclaracao tipoParametro = LASemanticoUtils.verificarTipo(escopo, parametro.tipo_variavel());

                // Adiciona os parâmetros na tabela de dados da função/procedimento.
                LASemanticoUtils.adicionarParametroNaTabela(escopo, dadosParametros, parametro, tipoParametro);

                // Adiciona os parâmetros na tabela de variáveis da função/procedimento.
                LASemanticoUtils.adicionarParametroNaTabela(escopo, tabelaDentroFuncao, parametro, tipoParametro);
            }
        }

        // Adiciona a função/procedimento na tabela geral.
        tabelaForaFuncao.adicionar(nome, tipoDeclarado, dadosParametros);

        // Visita todos os nós filhos do contexto;
        super.visitDeclaracao_funcoes(ctx);

        for (CmdContext cmd: ctx.cmd()){
            if (cmd.cmdRetorne() != null && tipoDeclarado == TipoDeclaracao.PROCEDIMENTO){
                LASemanticoUtils.adicionarErroSemantico(cmd.start, "comando retorne nao permitido nesse escopo");
            }
        }

        // Remove a tabela do escopo da função.
        escopo.removerEscopo();

        return null;
    }

    @Override
    public Void visitDeclaracao_variaveis
    (
        Declaracao_variaveisContext ctx
    )
    {
        TabelaDeSimbolos tabelaAtual = escopo.escopoAtual();

        if (ctx.DECLARE() != null){
            LASemanticoUtils.adicionarVariaveisNaTabela(escopo, tabelaAtual, ctx.variavel());
        }
        if (ctx.TIPO() != null){
            LASemanticoUtils.adicionarRegistroNoEscopo(escopo, ctx.registro(), ctx.IDENT().getText(), true);
        }
        if (ctx.CONSTANTE() != null){
            TipoDeclaracao tipoConstante = LASemanticoUtils.verificarTipo(ctx.tipo_basico());

            tabelaAtual.adicionar(ctx.IDENT().getText(), tipoConstante);
        }
        return super.visitDeclaracao_variaveis(ctx);
    }

    @Override
    public Void visitIdentificador
    (
        IdentificadorContext ctx
    ) 
    {
        Boolean existeIdentificador = LASemanticoUtils.existeIdentificadorTodosEscopos(escopo, ctx);
        String nome = ctx.IDENT(0).getText();

        if (ctx.PONTO() != null){
                for (int i = 0; i < ctx.PONTO().size(); i++){
                    nome += "." + ctx.IDENT(i+1);
                }
            }
        
        if (
            !existeIdentificador && 
            !(ctx.parent.parent instanceof RegistroContext) &&
            !(ctx.parent.parent.parent instanceof Declaracao_funcoesContext)
        ){
            LASemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nome + " nao declarado" );
        }
        return super.visitIdentificador(ctx);
    }

    @Override
    public Void visitCmdAtribuicao
    (
        CmdAtribuicaoContext ctx
    ) 
    {
        if (LASemanticoUtils.existeIdentificadorTodosEscopos(escopo, ctx.identificador())){
            String nome = ctx.identificador().IDENT(0).getText();

            if (ctx.identificador().PONTO() != null){
                for (int i = 0; i < ctx.identificador().PONTO().size(); i++){
                    nome += "." + ctx.identificador().IDENT(i+1);
                }
            }

            TipoDeclaracao tipoAlvo = LASemanticoUtils.getTipoDeTodosEscopos(escopo, nome);

            
            if (ctx.PONTEIRO() != null){
                tipoAlvo = TipoDeclaracao.PONTEIRO;
            }

            if (tipoAlvo == TipoDeclaracao.INVALIDO){
                LASemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nome + " com tipo invalido");
            }
            else {
                TipoDeclaracao tipoExpressao = LASemanticoUtils.verificarTipo(escopo, ctx.expressao());
                
                if (
                    tipoExpressao == TipoDeclaracao.INVALIDO || 
                    !LASemanticoUtils.tiposCompativeis(tipoExpressao, tipoAlvo)
                ){
                    String prefixo = "";

                    if (ctx.PONTEIRO() != null){
                        prefixo = ctx.PONTEIRO().getText();
                    }

                    LASemanticoUtils.adicionarErroSemantico(
                        ctx.start,
                        "atribuicao nao compativel para "+ prefixo + ctx.identificador().getText()
                    );
                }
            }

        }

        return super.visitCmdAtribuicao(ctx);
    }

    @Override
    public Void visitCmdChamada
    (
        CmdChamadaContext ctx
    ) 
    {
        String nomeChamado = ctx.IDENT().getText();
        TabelaDeSimbolos dadosParametros = LASemanticoUtils.recuperarEstruturaTipo(nomeChamado, escopo);

        if (dadosParametros.tamanhoTabela() != ctx.expressao().size()){
            LASemanticoUtils.adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
        }

        else{
            for (int i = 0; i < ctx.expressao().size(); i++){
                if (LASemanticoUtils.tiposCompativeis(dadosParametros.recuperarTipoParametro(i), LASemanticoUtils.verificarTipo(escopo, ctx.expressao(i)))){
                    LASemanticoUtils.adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + nomeChamado);
                }
            }
        }

        return super.visitCmdChamada(ctx);
    }
}