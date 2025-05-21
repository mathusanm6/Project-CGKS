package com.github.cgks.choco;

import com.github.cgks.MinerTest;

public class ChocoMinerTest extends MinerTest {

    @Override
    protected ChocoMiner createMiner() {
        return new ChocoMiner();
    }
}