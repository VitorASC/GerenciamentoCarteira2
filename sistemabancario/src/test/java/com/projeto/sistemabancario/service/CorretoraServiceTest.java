package com.projeto.sistemabancario.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.projeto.sistemabancario.dto.request.CorretoraCadastroRequest;
import com.projeto.sistemabancario.exception.RegraNegocioException;
import com.projeto.sistemabancario.integration.cep.CepConsultationPort;
import com.projeto.sistemabancario.integration.cnpj.CnpjConsultationPort;
import com.projeto.sistemabancario.integration.cvm.CvmCorretoraValidationPort;
import com.projeto.sistemabancario.repository.AcaoRepository;
import com.projeto.sistemabancario.repository.CarteiraInvestimentoRepository;
import com.projeto.sistemabancario.repository.CorretoraRepository;

class CorretoraServiceTest {

	@Test
	void rejeitaCnpjComDvInvalidoAntesDeRepositoriosEIntegracoes() {
		CorretoraRepository corretoras = mock(CorretoraRepository.class);
		CnpjConsultationPort cnpj = mock(CnpjConsultationPort.class);
		CepConsultationPort cep = mock(CepConsultationPort.class);
		CvmCorretoraValidationPort cvm = mock(CvmCorretoraValidationPort.class);
		AcaoRepository acoes = mock(AcaoRepository.class);
		CarteiraInvestimentoRepository carteiras = mock(CarteiraInvestimentoRepository.class);
		CorretoraService service = new CorretoraService(corretoras, cnpj, cep, cvm, acoes, carteiras);

		var request = new CorretoraCadastroRequest("11.222.333/0001-80", "01001000", null, null);

		assertThatThrownBy(() -> service.cadastrar(request))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessage("CNPJ inválido.");
		verifyNoInteractions(corretoras, cnpj, cep, cvm, acoes, carteiras);
	}
}
