package com.NexGen.nutriiftm.service;

import org.springframework.stereotype.Service;

import com.NexGen.nutriiftm.repository.ElementoRepository;
import com.NexGen.nutriiftm.model.Elemento;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ElementoService {
    private final ElementoRepository repo;

    public List<Elemento> listarTodos() {
        return repo.findAll();
    }

    public Elemento buscarPorId(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public Elemento salvar(Elemento elemento) {
        return repo.save(elemento);
    }

    public void deletar(Long id) {
        repo.deleteById(id);
    }

    public boolean existeOrdem(int ordem) {
        return repo.existsByEleOrdem(ordem);
    }

    public boolean existeOrdemEmOutroElemento(int ordem, Long codigo) {
        return repo.existsByEleOrdemAndEleCodigoNot(ordem, codigo);
    }
}
