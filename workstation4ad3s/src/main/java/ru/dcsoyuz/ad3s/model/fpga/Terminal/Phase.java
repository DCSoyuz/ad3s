package ru.dcsoyuz.ad3s.model.fpga.Terminal;


/**
 * Created by 1 on 18.11.2016.
 */
public enum Phase {

    ABC(0),
    ACB(1),
    BAC(2),
    BCA(3),
    CBA(4),
    CAB(5);


    Integer number;

    Phase(Integer number) {
        this.number = number;
    }

    public static Phase getPhase(Integer num){
        for(Phase phase : Phase.values()){
            if(phase.getNumber().equals(num)){
                return phase;
            }
        }
        return null;
    }

    public Integer getNumber() {
        return number;
    }
}
