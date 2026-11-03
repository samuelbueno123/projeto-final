package com.cefet_rj.projeto_final.assistant.services;

import com.cefet_rj.projeto_final.assistant.dtos.AssistantQueryDTO;
import com.cefet_rj.projeto_final.assistant.dtos.AssistantQueryDTO.AssistantResponseDTO;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CondoAssistantService {

    private final CondominiumAccessService condominiumAccessService;

    public AssistantResponseDTO answerQuestion(Long condominiumId, AssistantQueryDTO query) {
        condominiumAccessService.validateAccess(condominiumId);

        String rawQuestion = query.question();
        String normalized = normalize(rawQuestion);

        if (normalized.contains("barulho") || normalized.contains("silencio") || normalized.contains("som alto") || normalized.contains("festa")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "Conforme as normas condominiais e o Código Civil (Art. 1.336, IV), o período de silêncio rigoroso vigora das 22h00 às 08h00. " +
                    "Mesmo durante o dia, ruídos excessivos que perturbem o sossego, saúde e segurança dos vizinhos não são permitidos. " +
                    "Infrações estão sujeitas a notificação por escrito e multas cominatórias progressivas deliberadas em convenção.",
                    "Código Civil Brasileiro (Art. 1.277 e Art. 1.336, IV) e Regimento Interno Padrão.",
                    List.of(
                            "Como registrar uma ocorrência de barulho no aplicativo?",
                            "Qual o valor da multa por descumprimento do horário de silêncio?",
                            "Quais os horários permitidos para obras e reformas?"
                    )
            );
        }

        if (normalized.contains("mudanca") || normalized.contains("mudancas") || normalized.contains("caminhao")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "Mudanças e transporte de grandes volumes devem ser agendados com antecedência mínima de 48 horas junto à administração/portaria. " +
                    "Horários autorizados: Segunda a Sexta das 08h00 às 18h00, e Sábados das 08h00 às 13h00. É proibida realização de mudanças aos domingos e feriados. " +
                    "É obrigatório o uso de capas de proteção no elevador de serviço.",
                    "Regimento Interno Condominial - Capítulo de Mudanças e Logística.",
                    List.of(
                            "Como agendar o uso do elevador para mudança?",
                            "Qual a altura máxima do caminhão na rampa de acesso?",
                            "É cobrada taxa de uso para mudanças?"
                    )
            );
        }

        if (normalized.contains("pet") || normalized.contains("cachorro") || normalized.contains("gato") || normalized.contains("animal") || normalized.contains("animais")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "A permanência de animais de estimação na unidade autônoma é assegurada pela jurisprudência do Superior Tribunal de Justiça (STJ). " +
                    "Entretanto, nas áreas comuns é obrigatório o trânsito com guia e coleira curta (focinheira para raças de grande porte ou previstas em lei estadual). " +
                    "O tutor deve recolher imediatamente dejetos e evitar latidos contínuos que perturbem o sossego. Não é permitido o ingresso de pets em piscinas e salões de festa.",
                    "Recurso Especial STJ REsp 1.783.076 e Código Civil Art. 1.336.",
                    List.of(
                            "Onde posso passear com meu pet no condomínio?",
                            "Posso cadastrar meu pet no aplicativo do condomínio?",
                            "Como reportar sujeira ou animal solto nas áreas comuns?"
                    )
            );
        }

        if (normalized.contains("reforma") || normalized.contains("obra") || normalized.contains("art") || normalized.contains("rrt") || normalized.contains("engenheiro")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "Qualquer obra ou intervenção estrutural na unidade requer comunicação prévia ao síndico e entrega de ART (Anotação de Responsabilidade Técnica) ou RRT emitida por profissional habilitado, conforme a norma ABNT NBR 16.280. " +
                    "Horário permitido para ruído de obras: Segunda a Sexta das 08h00 às 17h00. Descarte de entulho é de exclusiva responsabilidade do condômino em caçamba autorizada.",
                    "ABNT NBR 16.280 (Reforma em Edificações) e Código Civil Art. 1.336, II.",
                    List.of(
                            "Como enviar o documento de ART/RRT pelo aplicativo?",
                            "Posso usar a caçamba de entulho na calçada do condomínio?",
                            "O síndico pode embargar uma obra irregular?"
                    )
            );
        }

        if (normalized.contains("vaga") || normalized.contains("garagem") || normalized.contains("carro") || normalized.contains("estacionamento") || normalized.contains("moto")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "As vagas de garagem destinam-se exclusivamente ao estacionamento de veículos automotores ou motos devidamente cadastrados. " +
                    "É proibido utilizar a vaga como depósito de móveis, caixas ou materiais de obra. " +
                    "A velocidade máxima na garagem é de 10 km/h com faróis baixos. A locação de vagas para pessoas estranhas ao condomínio é proibida pelo Código Civil, salvo autorização expressa na convenção.",
                    "Código Civil Art. 1.331, § 1º e Art. 1.338.",
                    List.of(
                            "Como cadastrar uma nova placa de veículo no sistema?",
                            "O que fazer se outro veículo estacionar na minha vaga?",
                            "É permitido instalar carregador de carro elétrico na vaga?"
                    )
            );
        }

        if (normalized.contains("encomenda") || normalized.contains("pacote") || normalized.contains("entrega") || normalized.contains("correios")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "As encomendas são recebidas e protocoladas pela equipe da portaria. Ao registrar o pacote, o sistema envia uma notificação instantânea para o morador. " +
                    "A retirada deve ser feita na guarita ou locker inteligente mediante apresentação do documento de identificação ou confirmação via aplicativo.",
                    "Procedimento Operacional Padrão da Portaria e Segurança.",
                    List.of(
                            "Onde vejo minhas encomendas pendentes?",
                            "Entregadores de delivery de comida podem subir até o apartamento?",
                            "Qual o prazo máximo para retirar uma encomenda na portaria?"
                    )
            );
        }

        if (normalized.contains("assembleia") || normalized.contains("votar") || normalized.contains("votacao") || normalized.contains("inadimplente") || normalized.contains("quórum") || normalized.contains("quorum")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "As assembleias gerais ordinárias ocorrem anualmente para aprovação de contas e eleição do síndico, enquanto extraordinárias tratam de pautas urgentes. " +
                    "Para participar das deliberações e votar nas enquetes e pautas da assembleia, a unidade deve estar em dia com suas taxas condominiais (adimplente), nos termos do Artigo 1.335, III do Código Civil.",
                    "Código Civil Art. 1.335, III e Art. 1.350 a 1.355.",
                    List.of(
                            "Como acessar a pauta de votação da próxima assembleia?",
                            "Posso votar por procuração na assembleia?",
                            "Como consultar se minha unidade possui faturas pendentes?"
                    )
            );
        }

        if (normalized.contains("lixo") || normalized.contains("reciclagem") || normalized.contains("coleta") || normalized.contains("organico")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "O condomínio dispõe de coleta seletiva. O lixo orgânico deve ser acondicionado em sacos plásticos resistentes e bem vedados. " +
                    "Materiais recicláveis (papel, plástico, metal, vidro limpos) devem ser depositados nos coletores identificados. " +
                    "Vidros quebrados e objetos perfurocortantes devem ser devidamente embalados em caixas ou garrafas PET para resguardar a integridade física dos funcionários.",
                    "Manual de Sustentabilidade e Resíduos Sólidos do Condomínio.",
                    List.of(
                            "Quais os dias de coleta de lixo reciclável?",
                            "Onde descartar óleo de cozinha e pilhas usadas?",
                            "É permitido deixar sacos de lixo no corredor do andar?"
                    )
            );
        }

        if (normalized.contains("comunicado") || normalized.contains("minuta") || normalized.contains("redigir") || normalized.contains("aviso")) {
            return new AssistantResponseDTO(
                    rawQuestion,
                    "Modelo Sugerido de Comunicado Oficial:\n\n" +
                    "\"COMUNICADO CONDOMINIAL IMPORTANTE\n\n" +
                    "Prezados condôminos e moradores,\n\n" +
                    "Informamos que no próximo dia [Data], das [Início] às [Fim], será realizada [Assunto: Ex. manutenção preventiva do reservatório de água / dedetização das áreas comuns].\n\n" +
                    "Solicitamos a colaboração de todos para [Recomendações: Ex. manter janelas fechadas / economizar consumo].\n\n" +
                    "Em caso de dúvidas, nossa administração permanece à disposição pelo canal oficial no aplicativo.\n\n" +
                    "Atenciosamente,\nAdministração do Condomínio.\"",
                    "Modelo Padrão de Comunicação Institucional Condominial.",
                    List.of(
                            "Como publicar este aviso no mural do aplicativo?",
                            "Como enviar notificação urgente para todos os moradores?",
                            "Como anexar um documento PDF ao comunicado?"
                    )
            );
        }

        // Resposta padrão contextualizada e abrangente
        return new AssistantResponseDTO(
                rawQuestion,
                "Como Assistente Inteligente do Condomínio, posso orientar condôminos, síndicos e portaria sobre regras do Regimento Interno, Convenção, Código Civil e rotinas do condomínio. " +
                "Você pode me consultar sobre horários de silêncio, procedimentos de mudança, regras de animais de estimação, reformas estruturais e ART, vagas de garagem, recebimento de encomendas, assembleias e deliberações, ou minutas de comunicados.",
                "Convenção Condominial, Regimento Interno e Código Civil Brasileiro (Lei Federal 10.406/2002).",
                List.of(
                        "Quais são os horários de silêncio e multas?",
                        "Como funciona o agendamento de mudanças?",
                        "Quais os documentos necessários para iniciar uma reforma?",
                        "Como consultar o resumo financeiro do condomínio?"
                )
        );
    }

    private String normalize(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
