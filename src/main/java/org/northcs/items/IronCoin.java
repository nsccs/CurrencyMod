package org.northcs.items;

public class IronCoin extends CoinBase {
    public IronCoin(Settings settings) {
        super(settings);
    }

    @Override
    public int amount() {
        return 5;
    }
}
