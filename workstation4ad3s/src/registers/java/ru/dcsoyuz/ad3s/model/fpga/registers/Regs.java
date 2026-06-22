package ru.dcsoyuz.ad3s.model.fpga.registers;

import java.util.ArrayList;
import java.util.List;

// https://www.ibm.com/docs/en/wxs/8.6.0?topic=applications-java-c-data-type-equivalents
public enum Regs implements IReg{

    KampS ("KampS [15:0] – коэффициент усиления по каналу АЦП IOSA1" +
            "(преобразователь 1), IOSA2 (преобразователь 2). Беззнаковое значение, всегда " +
            "положительное. Амплитуда сигналов на этих входах микросхемы умножается " +
            "на значение из данного регистра и делится на 1024. Значение по умолчанию " +
            "соответствует амплитуде сигнала на входе равному входному диапазону АЦП " +
            "(0 ÷ 2,5 В) для режима СКВТ. При изменении значений в данном регистре необходимо " +
            "следить за флагами переполнения, а также за срабатыванием компараторов порогов. ",
            RegValueType.VALUE15_0_UNSIGNED, "cc_kamps", false, 0, 1024),
    KampC ("KampC [15:0] – коэффициент усиления по каналу АЦП IOCA1 " +
            "(преобразователь 1), IOCA2 (преобразователь 2). Беззнаковое значение, всегда " +
            "положительное. Амплитуда сигналов на этих входах микросхемы умножается " +
            "на значение из данного регистра и делится на 1024. Значение по умолчанию " +
            "соответствует амплитуде сигнала на входе равному входному диапазону АЦП " +
            "(0 ÷ 2,5 В) для режима СКВТ. При изменении значений в данном регистре необходимо " +
            "следить за флагами переполнения, а также срабатыванием компараторов порогов. " +
            "В режиме Sensor_mode=01 максимальное значение 1024. ",
            RegValueType.VALUE15_0_UNSIGNED, "cc_kampc", false, 1,1024),
    KbiasS ("KbiasS [15:0] – смещение нуля по каналу АЦП IOSA1 (преобразователь 1), " +
            "IOSA2 (преобразователь 2). Знаковое значение в дополнительном коде. Максимум " +
            "+32767, минимум -32767.",
            RegValueType.VALUE15_0_SIGNED ,"cc_kbiass", false,2,0),
    KbiasC ("KbiasC [15:0] – смещение нуля по каналу АЦП IOCA1 (преобразователь 1), " +
            "IOCA2 (преобразователь 2). Знаковое значение в дополнительном коде. Максимум " +
            "+32767, минимум -32767.",
            RegValueType.VALUE15_0_SIGNED, "cc_kbiasc", false,3,0),
    fbias ("fbias[15:0] – коррекция неортогональности обмоток СКВТ. Смещение фазы " +
            "обмотки sin. Знаковое значение в дополнительном коде.",
            RegValueType.VALUE15_0_SIGNED ,"cc_phibias", false,4, 0),
    ExPhShft("ExPhShft[15:0] – задает сдвиг по фазе сигнала с EXO1 (преобразователь 1), " +
            "EXO2 (преобразователь 2) на плате до входов IOSA1, IOCA1 (преобразователь 1), " +
            "IOSA2, IOCA2 (преобразователь 2). Используется для определения квадранта " +
            "положения СКВТ. Знаковое значение в дополнительном коде.",
            RegValueType.VALUE15_0_UNSIGNED, "cc_exphshft", false,5, 0),
    ExoStngs ("Настройки Amp_code",
            RegValueType.VALUE_FIELDS, "cc_exostngs" , false,6, 0),
    EXInc ("EXInc[15:0] задает приращение фазы синусоидального сигнала на каждом " +
            "такте ЦАП (Fclk). Значение рассчитывается по формуле #. Значение " +
            "по умолчанию соответствует частоте 12 кГц.",
            RegValueType.VALUE15_0_UNSIGNED, "cc_exinc", false, 7 ,11500),

    Amp_th("Порог компаратора для вычисления флагов UIN в регистре C1Stat " +
            "(преобразователь 1), С2Stat (преобразователь 2). " ,
            RegValueType.VALUE_FIELDS, "cc_ampth",  false, 8,250),

    InputStngs("Опции",
            RegValueType.VALUE_FIELDS, "cc_inputstngs",false, 9, 300),

    Lock_th("Порог компаратора для вычисления флага NLock в регистре C1Stat " +
            "(преобразователь 1), С2Stat (преобразователь 2). Lock_th безразмерная " +
            "относительная величина. Рекомендуется подобрать значение этой величины такой, " +
            "чтобы при нормальной работе преобразователя не возникало срабатывание " +
            "компаратора.",
            RegValueType.VALUE15_0_UNSIGNED, "cc_lockth",false, 10, 300),





    Zero ("Zero[15:0] – коррекция вычисленной координаты. Значение Zero " +
            "прибавляется к вычисленной преобразователем координате. При коррекции угол " +
            "представлен 16-битным значением, вне зависимости от настроек, заданных " +
            "в C1ResCntrl или C2ResCntrl.",
            RegValueType.VALUE15_0_UNSIGNED, "cc_zero", false,11,0),
    Mask("Регистр маски Mask " +
            "Значение, записанное в регистр маски Mask, включает работу " +
            "соответствующих бит регистра C1Stat.",
            RegValueType.VALUE_FIELDS,"cc_mask",  false,12),
    KonturStngs("Регистры режимов работы преобразователей",
            RegValueType.VALUE_FIELDS , "cc_konturstngs", false,13),
    ResCntrl ("Регистр настройки выходной информации преобразователя",
            RegValueType.VALUE_FIELDS ,"cc_rescntrl", false,14),
    Vcnt_bound("Порог переполнения виртуального счетчика в старших разрядах [15:0]",
            RegValueType.VALUE15_0_UNSIGNED ,"cc_vcnt_bound", false,15, 0xFFFF),
    Coord ("Coord – координата, вычисленная в преобразователе. Разрядность " +
            "зависит от настроек в регистрах C1ResCntrl и C2ResCntrl.",
            RegValueType.VALUE15_0_UNSIGNED,  "KONTUR_ANGLE",  true,16),
    CoordHB ("CoordHB[11..0] - старшие 12 разрядов координаты" ,
            RegValueType.VALUE15_0_UNSIGNED,"KONTUR_ANGLE_HB", true,17, 0x1000),


    AdcS("Выход каналов АЦП и опорных сигналов",
            RegValueType.VALUE_FIELDS,"ADC_SIN_D_SYNCH", true,18),
    AdcC("Выход каналов АЦП и опорных сигналов",
            RegValueType.VALUE_FIELDS, "ADC_COS_D_SYNCH",true,19),

    OutS("Код канала АЦП SIN после коррекции смещения и амплитуды. " +
            "Является входным аргументом в контур.",
            RegValueType.VALUE_FIELDS,"arg_sin_kontur1", true,20),

    VirtualS("Виртуальные значения Sin участвующие в свертке",
            RegValueType.VALUE_FIELDS,"VIRTUAL_SIN", true,21),

    Err_metric ("Метрика ошибки угла на выходе микросхемы. По модулю этой метрики срабатывает " +
            "компаратор флага NLock.",
            RegValueType.VALUE15_0_SIGNED , "CC_ERR_METRIC", true,22),

    Amp_metric("Метрика амплитуды сигнала на входе микросхемы. По этой метрике " +
            "срабатывают компараторы флагов UIN_High, UIN_Low. Номинальное значение 400.",
            RegValueType.VALUE11_0_UNSIGNED,"CC_AMPMETRIC",  true,23, 0x8),

    Vel ("Vel – скорость, вычисленная в преобразователе. Разрядность зависит " +
            "от настроек в регистрах C1ResCntrl и C2ResCntrl.",
            RegValueType.VALUE15_0_SIGNED, "KONTUR_VEL", true,24),
    VelHB ("Старшие разряды скорости Vel",
            RegValueType.VALUE15_0_SIGNED,"KONTUR_VEL_HB", true,25),
    PhiS("Выход контура с учетом модели датчика, коэффициентов InDelay, KbiasS, Fbias",
            RegValueType.VALUE_FIELDS   ,"PHIMODEL_SIN", true,26),
    PhiC("Выход контура с учетом модели датчика, коэффициентов InDelay, KbiasC, Fbias",
            RegValueType.VALUE_FIELDS, "PHIMODEL_COS",true,27),
    OutC("Код канала АЦП COS после коррекции смещения и амплитуды. " +
            "Является входным аргументом в контур.",
            RegValueType.VALUE_FIELDS, "arg_cos_kontur1",true,28),
    VirtualC("Виртуальные значения Cos для вычисления ошибки в контуре",
            RegValueType.VALUE_FIELDS, "VIRTUAL_COS",true,29),
    Stat ("Stat - регистр ошибок/состояния канала преобразователя.",
            RegValueType.VALUE_FIELDS ,"CC_STAT", true,30, 0x1),
    Pole_addi("Корректировка номера полюса [11:0]. Значение добавляется к виртуальному счетчику. ",
            RegValueType.VALUE11_0_UNSIGNED,"cc_pole_addi", false,31, 0),

    IC_addr("Текущий адрес запросов к устройству " +
            "Для того чтобы микросхема принимала и выдавала " +
            "значения, необходимо установить BUS_addr = 0 или BUS_addr == IC_addr",
            RegValueType.VALUE7_0_UNSIGNED, "сс_mcu_addr",false,0,0),


    ADC_config("Настройки периода работы преобразователей и частоты тактирования АЦП. " +
            " Период работы преобразователя вычисляется по следующей формуле: " +
            "  Tclk = 16 * Tclk_adc + Delay_cycles*Tfint " +
            "где FINT - частота тактирования цифрового блока (внешняя или с PLL) " +
            "Tclk_adc = 1/Fclk_adc, Fadc = FINT/(FINT_divisor+1) ",
            RegValueType.VALUE_FIELDS, "cc_adcconfig",false,1),
    Mask_Stat("маски для регистров C1Stat и C2Stat",
            RegValueType.VALUE_FIELDS , "cc_mask_stat", false,2),
    Flags_delay ("Flags_delay [15:0] выполняет следующие функции: " +
            "задает время обновления регистров Amp_metric и флагов UIN_HIGH, " +
            "UIN_LOW (с ревизии 3); " +
            "устанавливает время удержания флагов. Единица времени 4/Fclk. После " +
            "пропадания ошибки время удержания флагов (3×65535 – 4×65535) мкс. " +
            "Желательно устанавливать время удержания флагов больше периода " +
            "сигнала возбуждения датчика, чтобы избежать постоянного сброса и обратной " +
            "установки флагов. В то же время, установка слишком большого значения " +
            "нежелательна, т.к. время обновления флагов увеличивается.",
            RegValueType.VALUE15_0_UNSIGNED, "cc_flagsdelay",false, 3,0),
    WR_lock("Блокировка записи настроек в микросхему. "+
            "Если WR_lock == 0, запись разрешена. " +
            "При  WR_lock != 0, команды записи не исполняются ",
            RegValueType.VALUE15_0_UNSIGNED,"cc_tstreg", false,4, 0),
    CMP_lth("Максимальное допустимое различие результатов преобразования каналов 1 " +
            "и 2 для выставления флага Not_Equal в регистре Stat_main.",
            RegValueType.VALUE15_4_UNSIGNED , "cc_cmplth",false, 5,1),

    AFE_config ("Регистр настройки аналоговых блоков",
            RegValueType.VALUE_FIELDS , "cc_afeconfig", false,6),
    Mode_config("Регистр общей настройки микросхемы",
            RegValueType.VALUE_FIELDS ,"cc_modeconfig",  false,7),


    NOCLK_stat ( " Биты состояния микросхемы",
            RegValueType.VALUE_FIELDS,"NOCLK_state", true,8),

    SPI_req  ("Предыдущая транзакция SPI",
            RegValueType.VALUE15_0_UNSIGNED,"SPI_req", true,9),

    alive_cnt ("alive_cnt [15:0] – счетчик считает во время работы микросхемы. " +
            " Единица времени 32768/Fclk",
            RegValueType.VALUE15_0_UNSIGNED,"CC_ALIVE_CNT", true,10),

    Stat_main("Регистр состояния микросхемы.",
            RegValueType.VALUE_FIELDS,"CC_MODESTAT_FLAGS", true,11),
    Dcpu1LB ("Выходная шина с регистров CPU1, младшее слово.",
            RegValueType.VALUE15_0_UNSIGNED,"CPU1_D_DBG", true,12),
    Dcpu1HB ("Выходная шина с регистров CPU1, старшее слово.",
            RegValueType.VALUE15_0_UNSIGNED,"CPU1_D_DBG", true,13),
    Dcpu2LB ("Выходная шина с регистров CPU2, младшее слово.",
            RegValueType.VALUE15_0_UNSIGNED,"CPU2_D_DBG", true,14),
    Dcpu2HB ("Выходная шина с регистров CPU2, старшее слово.",
            RegValueType.VALUE15_0_UNSIGNED,"CPU2_D_DBG", true,15),

    PLL_config ("Регистр настройки режимов тактирования микросхемы",
            RegValueType.VALUE_FIELDS,"cc_pllconfig", false,0),
    INIT_conf   ("Регистр управления режимом начальной конфигурации микросхемы",
            RegValueType.VALUE_FIELDS,"cc_init_conf", false,1),
    UOTP_ctrl  ("Регистр управления записи и чтения пользовательской памяти " +
            "прямого доступа (UOTP) , отвечает за регистры PLL_CONFIG, INIT_conf",
            RegValueType.VALUE_FIELDS,"cc_uotp_ctrl", false,2),
    BUS_addr("Текущий адрес запросов к устройству " +
            "Если задан IC_addr, чтобы микросхема принимала и выдавала " +
            "значения необходимо установить BUS_addr = 0 или BUS_addr == IC_addr",
            RegValueType.VALUE7_0_UNSIGNED, "сс_bus_addr",false,3,0),
    BOTP_addr("Регистр адреса блока OTP памяти 512x16 бит (BOTP)",
            RegValueType.VALUE8_0_UNSIGNED,"cc_botp_addr", false,4, 0),
    BOTP_data("Регистр данных блока OTP памяти 512x16 бит (BOTP)",
            RegValueType.VALUE15_0_SIGNED, "cc_botp_data", false,5, 0),

    BOTP_ctrl("Регистр управления записи и чтения " +
            "блока OTP памяти 512x16 бит (BOTP)",
            RegValueType.VALUE_FIELDS,"cc_botp_ctrl", false,6),
    BOTP_out("Выход блока OTP памяти 512x16 бит (BOTP)",
            RegValueType.VALUE15_0_UNSIGNED,"BOTP_OUT", true,7),

    DBG_ctrl("Регистр управления отладкой CPU",
            RegValueType.VALUE_FIELDS,null, false,0),
    DBG_data("Данные управления отладкой CPU",
            RegValueType.VALUE_FIELDS,null, false,1);



    RegValueType valueType;

    boolean onlyRead;

    int localAddr;

    String description;

    String hdlName;

    Regs(String description, RegValueType valueType, String hdlName,  boolean onlyRead, int localAddr) {
        this.description = description;
        this.valueType = valueType;
        this.hdlName = hdlName;
        this.onlyRead = onlyRead;
        this.localAddr = localAddr;
        this.defaultValue = null;

    }
    Regs(String description, RegValueType valueType, String hdlName, boolean onlyRead, int localAddr, int defaultValue) {
        this.description = description;
        this.valueType = valueType;
        this.hdlName = hdlName;
        this.defaultValue = defaultValue;
        this.onlyRead = onlyRead;
        this.localAddr = localAddr;
    }

    private Integer defaultValue = 0;

    public RegValueType getValueType() {
        return valueType;
    }

    public Integer getDefaultValue() {
        if(valueType.equals(RegValueType.VALUE_FIELDS)){
            return  RegField.getRegDefaultValue(this);
        }else {
            return defaultValue;
        }

    }


    public List<IRegField> getFields(){
        List<IRegField> list = new ArrayList<>();
        for(RegField field : RegField.values()){
            if(field.reg.equals(this)){
                list.add(field);
            }
        }
        return list;
    }

    public int getLocalAddr() {
        return localAddr;
    }

    public boolean isOnlyRead() {
        return onlyRead;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return name();
    }

    public String getHdlName() {
        return hdlName;
    }
}
