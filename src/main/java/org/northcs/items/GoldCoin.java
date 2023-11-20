package org.northcs.items;

public class GoldCoin extends CoinBase {
    public GoldCoin(Settings settings) {
        super(settings);
    }

    @Override
    public int amount() {
        return 10;
    }
}
