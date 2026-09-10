package com.projeto.sistemabancario.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.sistemabancario.dto.request.CorretoraCadastroRequest;
import com.projeto.sistemabancario.dto.response.CorretoraResponse;
import com.projeto.sistemabancario.exception.DuplicateResourceException;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.exception.ResourceNotFoundException;
import com.projeto.sistemabancario.integration.cep.CepConsultationPort;
import com.projeto.sistemabancario.integration.cnpj.CnpjConsultationPort;
import com.projeto.sistemabancario.integration.cvm.CvmCorretoraValidationPort;
import com.projeto.sistemabancario.domains.entity.Corretora;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;
import com.projeto.sistemabancario.util.Documentos;

@Service
public class CorretoraService {

	private final CorretoraRepository corretoraRepository;
	private final CnpjConsultationPort cnpjConsultationPort;
	private final CepConsultationPort cepConsultationPort;
	private final CvmCorretoraValidationPort cvmCorretoraValidationPort;
	private final AcaoRepository acaoRepository;
	private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;

	public CorretoraService(CorretoraRepository corretoraRepository, CnpjConsultationPort cnpjConsultationPort,
			CepConsultationPort cepConsultationPort, CvmCorretoraValidationPort cvmCorretoraValidationPort,
			AcaoRepository acaoRepository, CarteiraInvestimentoRepository carteiraInvestimentoRepository) {
		this.corretoraRepository = corretoraRepository;
		this.cnpjConsultationPort = cnpjConsultationPort;
		this.cepConsultationPort = cepConsultationPort;
		this.cvmCorretoraValidationPort = cvmCorretoraValidationPort;
		this.acaoRepository = acaoRepository;
		this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
	}

	@Transactional
	public CorretoraResponse cadastrar(CorretoraCadastroRequest request) {
		String cnpj = Documentos.apenasDigitos(request.cnpj());
		if (!Documentos.cnpjValido(request.cnpj())) {
			throw new RegraNegocioException("CNPJ inválido.");
		}
		if (corretoraRepository.existsByCnpj(cnpj)) {
			throw new DuplicateResourceException("Já existe corretora cadastrada com este CNPJ.");
		}

		var dadosCnpj = cnpjConsultationPort.consultar(cnpj).orElseThrow(
				() -> new RegraNegocioException("CNPJ não encontrado na base pública consultada (BrasilAPI)."));

		String cepInformado = Documentos.apenasDigitos(request.cep());
		if (cepInformado.length() != 8) {
			throw new RegraNegocioException("CEP deve conter 8 dígitos.");
		}
		var dadosCep = cepConsultationPort.consultar(cepInformado).orElseThrow(
				() -> new RegraNegocioException("CEP não pôde ser validado na API pública de CEP."));

		String cepReceita = Documentos.apenasDigitos(dadosCnpj.cep());
		if (cepReceita.length() == 8 && !cepReceita.equals(cepInformado)) {
			throw new RegraNegocioException(
					"O CEP informado não coincide com o CEP cadastrado na Receita Federal para este CNPJ.");
		}

		var cvm = cvmCorretoraValidationPort.validar(cnpj);
		boolean validadaNaCvm = cvm.encontradoNoCadastroCvmComoCorretora() && cvm.emFuncionamentoNormal();

		Corretora corretora = new Corretora();
		corretora.setCnpj(cnpj);
		corretora.setRazaoSocial(dadosCnpj.razaoSocial());
		corretora.setNomeFantasia(dadosCnpj.nomeFantasia());
		corretora.setEmail(dadosCnpj.email());
		corretora.setTelefone(dadosCnpj.telefone());
		corretora.setSituacaoCadastral(dadosCnpj.situacaoCadastral());

		corretora.setCep(dadosCep.cep());
		corretora.setLogradouro(dadosCep.logradouro());
		corretora.setBairro(dadosCep.bairro());
		corretora.setCidade(dadosCep.cidade());
		corretora.setUf(dadosCep.uf());
		corretora.setNumero(request.numero());
		corretora.setComplemento(request.complemento() != null ? request.complemento() : dadosCep.complemento());

		corretora.setValidadaNaCvm(validadaNaCvm);
		corretora.setDataCadastro(LocalDateTime.now());

		return toResponse(corretoraRepository.save(corretora));
	}

	@Transactional(readOnly = true)
	public Page<CorretoraResponse> listar(Pageable pageable) {
		return corretoraRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public CorretoraResponse buscarPorId(Long id) {
		Corretora c = corretoraRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada."));
		return toResponse(c);
	}

	@Transactional(readOnly = true)
	public CorretoraResponse buscarPorCnpj(String cnpjBruto) {
		String cnpj = Documentos.apenasDigitos(cnpjBruto);
		Corretora c = corretoraRepository.findByCnpj(cnpj)
			.orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada para o CNPJ informado."));
		return toResponse(c);
	}

	@Transactional
	public void excluir(Long id) {
		Corretora c = corretoraRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Corretora não encontrada."));

		if (acaoRepository.existsByCorretora_Id(id)) {
			throw new RegraNegocioException(
					"Não é possível excluir esta corretora: existem ações vinculadas a ela.");
		}
		if (carteiraInvestimentoRepository.existsByCorretora_Id(id)) {
			throw new RegraNegocioException(
					"Não é possível excluir esta corretora: existem carteiras vinculadas a ela.");
		}

		corretoraRepository.delete(c);
	}

	private CorretoraResponse toResponse(Corretora c) {
		return new CorretoraResponse(
				c.getId(),
				c.getCnpj(),
				c.getRazaoSocial(),
				c.getNomeFantasia(),
				c.getEmail(),
				c.getTelefone(),
				c.getCep(),
				c.getLogradouro(),
				c.getNumero(),
				c.getComplemento(),
				c.getBairro(),
				c.getCidade(),
				c.getUf(),
				c.getSituacaoCadastral(),
				c.getValidadaNaCvm(),
				c.getDataCadastro());
	}
}
