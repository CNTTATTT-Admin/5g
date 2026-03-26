package entity;

import java.util.List;

public class BitSequenceData {
    private List<Integer> aliceBits;
    private List<Integer> bobBits;
    private List<Integer> eveBits;

    public BitSequenceData() {
    }

    public BitSequenceData(List<Integer> aliceBits, List<Integer> bobBits, List<Integer> eveBits) {
        this.aliceBits = aliceBits;
        this.bobBits = bobBits;
        this.eveBits = eveBits;
    }

    public List<Integer> getAliceBits() {
        return aliceBits;
    }

    public void setAliceBits(List<Integer> aliceBits) {
        this.aliceBits = aliceBits;
    }

    public List<Integer> getBobBits() {
        return bobBits;
    }

    public void setBobBits(List<Integer> bobBits) {
        this.bobBits = bobBits;
    }

    public List<Integer> getEveBits() {
        return eveBits;
    }

    public void setEveBits(List<Integer> eveBits) {
        this.eveBits = eveBits;
    }
    
}
