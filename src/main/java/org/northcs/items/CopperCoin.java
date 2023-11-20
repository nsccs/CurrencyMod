package org.northcs.items;

public class CopperCoin extends CoinBase {
    public CopperCoin(Settings settings) {
        super(settings);
    }

    @Override
    public int amount() {
        return 1;
    }
}
