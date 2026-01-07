package com.poc.pocpdf.adapters.in.rest;

import com.poc.pocpdf.adapters.in.rest.dto.TemplateExistsResponseDto;
import com.poc.pocpdf.adapters.in.rest.dto.UpdateTemplateResponseDto;
import com.poc.pocpdf.application.port.in.UpdateTemplateUseCase;
import com.poc.pocpdf.application.port.out.TemplateStoragePort;
import com.poc.pocpdf.domain.event.TemplateVersionCreated;
import com.poc.pocpdf.domain.model.Clause;
import com.poc.pocpdf.domain.model.ContractName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class TemplateController {

    private final UpdateTemplateUseCase updateTemplateUseCase;
    private final TemplateStoragePort templateStoragePort;

    public TemplateController(UpdateTemplateUseCase updateTemplateUseCase,
                              TemplateStoragePort templateStoragePort) {
        this.updateTemplateUseCase = updateTemplateUseCase;
        this.templateStoragePort = templateStoragePort;
    }

    @Operation(
            summary = "Atualiza template de contrato e cria nova versão (vN), gerando também o PDF",
            description = """
                    Endpoint multipart para enviar um DOCX de template e opcionalmente cláusulas extras.
                    O serviço versiona como v1, v2, ... mantendo versões anteriores, e gera um PDF.

                    Envio via curl (Windows PowerShell):
                    curl.exe -X POST "http://localhost:8080/contracts/contrato-locacao/template" ^
                      -F "file=@D:\\\\POC\\\\POCPDF\\\\templates\\\\contrato-template.docx;type=application/vnd.openxmlformats-officedocument.wordprocessingml.document" ^
                      -F "clauses=O cliente concorda com ..." ^
                      -F "clauses=Fica estabelecido que ..."

                    Envio via curl (Linux/Mac):
                    curl -X POST "http://localhost:8080/contracts/contrato-locacao/template" \\
                      -F "file=@./templates/contrato-template.docx;type=application/vnd.openxmlformats-officedocument.wordprocessingml.document" \\
                      -F "clauses=O cliente concorda com ..." \\
                      -F "clauses=Fica estabelecido que ..."
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Template atualizado e versão gerada",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateTemplateResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "Sucesso",
                                            value = """
                                                    {
                                                      "contractName": "contrato-locacao",
                                                      "version": "v2",
                                                      "docxPath": "output/contratos/contrato-locacao/v2/contrato-locacao.docx",
                                                      "pdfPath": "output/contratos/contrato-locacao/v2/contrato-locacao.pdf"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Arquivo não enviado ou inválido"),
                    @ApiResponse(responseCode = "500", description = "Erro interno")
            }
    )
    @PostMapping(
            value = "/{contractName}/template",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UpdateTemplateResponseDto> updateTemplate(
            @Parameter(description = "Identificador do contrato", example = "contrato-locacao")
            @PathVariable String contractName,

            @Parameter(
                    description = "Arquivo DOCX do template",
                    required = true,
                    content = @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(
                    description = """
                            Lista de cláusulas extras. No multipart, envie repetindo o campo clauses.
                            Exemplo:
                            clauses=O cliente concorda com ...
                            clauses=Fica estabelecido que ...
                            """,
                    examples = {
                            @ExampleObject(name = "Duas cláusulas", value = "O cliente concorda com ..."),
                            @ExampleObject(name = "Outra cláusula", value = "Fica estabelecido que ...")
                    }
            )
            @RequestParam(value = "clauses", required = false) List<String> clauses
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] templateDocx = file.getBytes();

        List<Clause> extraClauses = (clauses == null)
                ? List.of()
                : clauses.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(Clause::new)
                .toList();

        TemplateVersionCreated created = updateTemplateUseCase.updateTemplate(
                new ContractName(contractName),
                templateDocx,
                extraClauses
        );

        UpdateTemplateResponseDto resp = new UpdateTemplateResponseDto(
                created.contractName().value(),
                created.version().asString(),
                created.docxPath(),
                created.pdfPath()
        );

        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Verifica se existe template atual para o contrato",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Resultado da verificação",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TemplateExistsResponseDto.class),
                            examples = @ExampleObject(value = """
                                    { "contractName": "contrato-locacao", "exists": true }
                                    """)
                    )
            )
    )
    @GetMapping(value = "/{contractName}/template/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public TemplateExistsResponseDto exists(
            @Parameter(description = "Identificador do contrato", example = "contrato-locacao")
            @PathVariable String contractName
    ) {
        boolean exists = templateStoragePort.exists(new ContractName(contractName));
        return new TemplateExistsResponseDto(contractName, exists);
    }

    @Operation(
            summary = "Baixa o template atual (DOCX)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Arquivo DOCX retornado"),
                    @ApiResponse(responseCode = "404", description = "Template não encontrado")
            }
    )
    @GetMapping(value = "/{contractName}/template")
    public ResponseEntity<byte[]> downloadCurrentTemplate(
            @Parameter(description = "Identificador do contrato", example = "contrato-locacao")
            @PathVariable String contractName
    ) {
        ContractName cn = new ContractName(contractName);

        if (!templateStoragePort.exists(cn)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = templateStoragePort.load(cn);
        String filename = cn.asKey() + ".docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
                .body(bytes);
    }
}
