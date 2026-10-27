package com.pelada_gestao.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pelada_gestao.domain.model.Jogador;
import com.pelada_gestao.domain.model.Sorteio;
import com.pelada_gestao.exception.custom.OperacaoNaoPermitidaException;
import com.pelada_gestao.repository.JogadorRepository;
import com.pelada_gestao.repository.JogadorSorteadoRepository;
import com.pelada_gestao.repository.SorteioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SorteioServiceImplTest {

    private final SorteioRepository sorteioRepository = mock(SorteioRepository.class);
    private final JogadorRepository jogadorRepository = mock(JogadorRepository.class);
    private final JogadorSorteadoRepository jogadorSorteadoRepository = mock(JogadorSorteadoRepository.class);
    private final SorteioServiceImpl service =
            new SorteioServiceImpl(sorteioRepository, jogadorRepository, jogadorSorteadoRepository);

    private Sorteio sorteio;

    @BeforeEach
    void setUp() {
        sorteio = new Sorteio();
        sorteio.setId(1L);
        sorteio.setJogadoresPorEquipe(3);
        when(sorteioRepository.findById(1L)).thenReturn(Optional.of(sorteio));
    }

    private List<Jogador> jogadores(int goleiros, int deLinha) {
        List<Jogador> lista = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < goleiros; i++) {
            lista.add(jogador(id++, true));
        }
        for (int i = 0; i < deLinha; i++) {
            lista.add(jogador(id++, false));
        }
        return lista;
    }

    private Jogador jogador(long id, boolean goleiro) {
        Jogador jogador = new Jogador();
        jogador.setId(id);
        jogador.setNome("Jogador " + id);
        jogador.setGoleiro(goleiro);
        return jogador;
    }

    @Test
    void cadaTimeRecebeUmGoleiroEOsDemaisVaoParaReserva() {
        when(jogadorRepository.findBySorteioId(1L)).thenReturn(jogadores(3, 6));

        Map<String, List<Jogador>> resultado = service.sortearTimes(1L);

        assertThat(resultado.get("timeAzul")).hasSize(3).filteredOn(Jogador::isGoleiro).hasSize(1);
        assertThat(resultado.get("timeVermelho")).hasSize(3).filteredOn(Jogador::isGoleiro).hasSize(1);
        assertThat(resultado.get("reservas")).hasSize(3);
    }

    @Test
    void nenhumJogadorFicaDeFora() {
        when(jogadorRepository.findBySorteioId(1L)).thenReturn(jogadores(0, 8));

        Map<String, List<Jogador>> resultado = service.sortearTimes(1L);

        int total = resultado.values().stream().mapToInt(List::size).sum();
        assertThat(total).isEqualTo(8);
        assertThat(resultado.get("reservas")).hasSize(2);
    }

    @Test
    void sorteioFicaMarcadoComoRealizado() {
        when(jogadorRepository.findBySorteioId(1L)).thenReturn(jogadores(2, 4));

        service.sortearTimes(1L);

        assertThat(sorteio.isSorteado()).isTrue();
        verify(sorteioRepository).save(sorteio);
        verify(jogadorSorteadoRepository, times(3)).saveAll(any());
    }

    @Test
    void naoSorteiaDuasVezes() {
        sorteio.setSorteado(true);

        assertThatThrownBy(() -> service.sortearTimes(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void resultadoExigeSorteioRealizado() {
        assertThatThrownBy(() -> service.resultadoDoSorteio(1L))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }
}
