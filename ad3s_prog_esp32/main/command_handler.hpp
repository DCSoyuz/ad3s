

#pragma once

#include "esp_log.h"
#include "soc/spi_struct.h"
#include "driver/spi_master.h"
#include "driver/uart.h"
#include "spi_maker.hpp"
#include "usb_cdc.hpp"
#include "ad3s_types.hpp"
#include "esp32_dma_spi_master.hpp"
#include "encoder_pcnt.hpp"

#include <string.h>
#include <thread>
#include <cstdlib>

#include "driver/gpio.h"
#include "freertos/queue.h"
#include "freertos/event_groups.h"

namespace idf {

static int fpgaMasterMode = 0;
static int milandrMode = 0;
static int sdiValueForDmaQuad = 0;
static int vcValueForDmaQuad = 0;
static int dmaQuadModeIsEna = 0;
static int dmaSingleModeIsEna = 0;
static uint16_t sizePbufDmaSingleBuffer;
static uint8_t *pbufDmaSingleBuffer;
static EventGroupHandle_t event_group_dma_quad;
static constexpr int DMA_QUAD_TRIGGER_BIT = (1 << 2);  // bit 2 — Java triggers one-shot read
static EventGroupHandle_t event_group_dma_single;
static EventGroupHandle_t event_group_record;
uint8_t *dma_tx_buf;
uint8_t *dma_rx_buf;
static int recordModeIsEna = 0;
static uint16_t record_num_addrs = 0;
static uint8_t* record_addr_buffer = nullptr;
static EncoderManagerPCNT* encoderManager;

/**
 * @brief The maximum SPI transfer size in bytes.
 */
class CommandHandler {



public:

   TaskHandle_t  cyclicDmaSingleHand;
   TaskHandle_t  cyclicRecordHand;

   CommandHandler(ESP32DMASPI::Master &m, USBCDC &u, spi_maker &s, QueueHandle_t p2,  QueueHandle_t u2 )
   :
       masterDmaSpi(m),
       usbcdc(u),
       spim(s),
       parsingPacket_queue(p2),
       uart_resp_queue(u2)
    {      

        gpio_set_direction((gpio_num_t)GPIO_NRESET, GPIO_MODE_OUTPUT); 
        gpio_set_level((gpio_num_t)GPIO_NRESET, 0);
        event_group_dma_quad = xEventGroupCreate();
        event_group_dma_single = xEventGroupCreate();
        event_group_record = xEventGroupCreate();
        xTaskCreate(main_task, "main_task", 4096, this, 3, NULL);
        xTaskCreate(cyclicSendDmaSingleTransaction, "cyclicSendDmaSingleTransaction", 4096, this, 11, &cyclicDmaSingleHand  );
        xTaskCreate(cyclicRecordTransaction,       "cyclicRecordTransaction"       , 4096, this, 8, &cyclicRecordHand      );
        
        gpio_set_direction((gpio_num_t)LED_PIN, GPIO_MODE_OUTPUT); 
        gpio_set_direction((gpio_num_t)STNDBY_PIN, GPIO_MODE_OUTPUT); 
        gpio_set_level((gpio_num_t)VPP9V_PIN, vpp9v_level(1));
        gpio_set_direction((gpio_num_t)VPP9V_PIN, GPIO_MODE_OUTPUT); 
        
        gpio_set_direction((gpio_num_t)GPIO_SAMPLE, GPIO_MODE_OUTPUT);
        gpio_set_direction((gpio_num_t)GPIO_VC, GPIO_MODE_OUTPUT); 
        gpio_set_direction((gpio_num_t)GPIO_NSEN, GPIO_MODE_OUTPUT); 
        gpio_set_level((gpio_num_t)STNDBY_PIN, 0);

        gpio_set_level((gpio_num_t)GPIO_SAMPLE, 0);
        gpio_set_level((gpio_num_t)GPIO_VC, 0);
        gpio_set_level((gpio_num_t)GPIO_NSEN, 0);
        gpio_set_level((gpio_num_t)GPIO_NRESET, 1);

        // Initialize PCNT encoder manager (created but NOT initialized yet)
        encoderManager = new EncoderManagerPCNT();
        // init() will be called on COMMAND_ENC_ON
    }


    ESP32DMASPI::Master &masterDmaSpi;
    USBCDC &usbcdc;
    spi_maker &spim;
    QueueHandle_t parsingPacket_queue;
    QueueHandle_t uart_resp_queue;


    int readCpuRegs (uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t  ){
        uint16_t numCpu = ( address & 0x0C00 ) >> 10; 
        uint16_t stopAddr2 =  address & 0x01FF ; 
        uint16_t enaStopAddr2 = ( address & 0x0200 ) >> 9; 
        uint8_t * bufAddrCpuRegValue = (uint8_t*) malloc(2);
        uint16_t address_cpu_reg = 10;
        uint8_t * dummyrvalues = (uint8_t*) malloc(2);
        uint16_t dbg_data = (address & 0x3FF) | (address_cpu_reg << 10);  
        ESP_LOGI(TAG, "address = %d regs command", address);
        ESP_LOGI(TAG, "read cpu%d regs command, stopAddress2 = %d, enaStopAddress2 = %d\n", numCpu, stopAddr2, enaStopAddr2 );
        uint16_t addressDbgData = 93;
        uint16_t addressDcpu = 76;
        if(numCpu == 2){
            addressDbgData = 95;
            addressDcpu = 78;
        }
        
        for(uint16_t i  = 0; i< 13; i++) {
            dbg_data = (address & 0x3FF) | (i << 10);  
            bufAddrCpuRegValue[0] = (uint8_t) (dbg_data & 0xFF);
            bufAddrCpuRegValue[1] = (uint8_t)((dbg_data >> 8) & 0xFF);
            spim.writeWordsToIc (addressDbgData, 1, &dummyrvalues[0], t, bufAddrCpuRegValue );
            spim.readWordsFromIc(   addressDcpu, 2, &rValues[i*4],    t );
        }
        return 0;
    }




    int readHandTapValues (uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues ){
        spi_transaction_t t1;
        memset(&t1, 0, sizeof(t1));
        int maxReadFlagCount = 10;
        uint8_t* pBufRespReadyFlag = (uint8_t*) malloc(2);
        uint16_t inFlag =  ((uint16_t ) wValues[1] << 8) | (uint16_t) wValues[0];
        uint16_t outFlag;
        spim.writeWordsToIc (760, 2, pBufRespReadyFlag, &t1, wValues );
        spi_transaction_t t2;
        memset(&t2, 0, sizeof(t2));

        for(int i = 0; i<maxReadFlagCount; i=i+1){
            spim.readWordsFromIc(748, 2, &pBufRespReadyFlag[0], &t2);
            outFlag = ((uint16_t ) pBufRespReadyFlag[1] << 8) | (uint16_t) pBufRespReadyFlag[0];
            //printf("inFlag: %04x\n", inFlag);
            //printf("outFlag: %04x\n", outFlag);
            if(outFlag == inFlag){
                break;
            }
        }

        spim.readWordsFromIc(608, 128, &rValues[0],   &t2);
        spim.readWordsFromIc(768, 128, &rValues[256], &t2);
        spim.readWordsFromIc(1280, 128, &rValues[512], &t2);
        return 0;
    }




        int static check_request (uartToMainQueueItem_t req){
            uint16_t size = ((uint16_t) * (req.pbuf+A_REQ_NUMB_H) << 8) + (uint16_t) * (req.pbuf+A_REQ_NUMB_L);
            
            if(size != req.len){
                ESP_LOGI(TAG, "Error: len request: %d don't equal value size packet %d",req.len, size); 
                return -1;
            }
            uint8_t check_sum = 0;
            for(uint16_t i = 0; i<req.len-1; i++ ){
                check_sum += (uint16_t) * (req.pbuf+i);
            }
            if(check_sum != *(req.pbuf+req.len-1)){
                ESP_LOGI(TAG, "Error: The received checksum [%02x] does not match the calculated [%02x]", *(req.pbuf+req.len-1),check_sum );
                return -1;
            } 
            return 0;
            
        }


    void static cyclicSendDmaSingleTransaction(void *pvParameters)
    {
        ESP_LOGI(TAG, "run task cyclicSendDmaSingleTransaction()");
        CommandHandler* obj = reinterpret_cast<CommandHandler*>(pvParameters);
        uartToMainQueueItem_t itemRespQueue;
        uint8_t* pBufResp = (uint8_t*) malloc(idf::WR_BUF_SIZE);
 
   
        pBufResp[idf::A_RESP_STARTBYTE] = (uint8_t) 0x55;

        pBufResp[A_RESP_COMMAND_WORD]   = (uint8_t) 0xFF;
        pBufResp[A_RESP_COMMAND_WORD+1] = (uint8_t) 0x00;
        pBufResp[A_RESP_ADDRESS_L]      = (uint8_t) 0x00;
        pBufResp[A_RESP_ADDRESS_H]      = (uint8_t) 0x00;

        const TickType_t xDelay = 1 / portTICK_PERIOD_MS;

        for(;;) {
            xEventGroupClearBits(event_group_dma_single, 2);
            EventBits_t bits = xEventGroupWaitBits(event_group_dma_single, 1, pdFALSE,pdTRUE, portMAX_DELAY);
            vTaskDelay( xDelay );
            // start and wait to complete one BIG transaction (same data will be received from slave)
            // show data in the range
       

            const size_t received_bytes = obj->masterDmaSpi.transfer(0,pbufDmaSingleBuffer, &pBufResp[A_RESP_VALUE0_L], (sizePbufDmaSingleBuffer<<1));
            int size_resp_packet = 9 + received_bytes;
            itemRespQueue.pbuf = pBufResp;
            itemRespQueue.len = (uint16_t) size_resp_packet;
            pBufResp[A_RESP_NUMB_L]         = (uint8_t) (size_resp_packet & 0xFF);
            pBufResp[A_RESP_NUMB_H]         = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
            pBufResp[A_RESP_NUMW_L]         = (uint8_t) ((sizePbufDmaSingleBuffer)& 0xFF) ; // SIZE in 16bit words
            pBufResp[A_RESP_NUMW_H]         = (uint8_t) ((sizePbufDmaSingleBuffer>>(8))& 0xFF);
     
            xQueueSend(obj->uart_resp_queue, &itemRespQueue, portMAX_DELAY);
            xEventGroupSetBits(event_group_dma_single, 2);
            vTaskDelay( xDelay );
    
        }

        vTaskDelete(NULL);

    }


    void static cyclicRecordTransaction(void *pvParameters)
    {
        ESP_LOGI(TAG, "run task cyclicRecordTransaction()");
        CommandHandler* obj = reinterpret_cast<CommandHandler*>(pvParameters);
        uint8_t* pBufResp = (uint8_t*) malloc(idf::WR_BUF_SIZE);
        spi_transaction_t t;
        memset(&t, 0, sizeof(t));

        const TickType_t xDelay = 1 / portTICK_PERIOD_MS;

        for(;;) {
            xEventGroupClearBits(event_group_record, 2);
            EventBits_t bits = xEventGroupWaitBits(event_group_record, 1, pdFALSE, pdTRUE, portMAX_DELAY);

            while(1) {
                EventBits_t stopBits = xEventGroupWaitBits(event_group_record, 1, pdFALSE, pdTRUE, 0);
                if(!(stopBits & 1)) {
                    break;
                }

                vTaskDelay( xDelay );

                // SPI read
                obj->spim.readRandomAddressValues(record_num_addrs, &pBufResp[A_RESP_VALUE0_L], &t, record_addr_buffer);

                // Build packet
                int size_resp_packet = record_num_addrs * 2 + 9;
                pBufResp[idf::A_RESP_STARTBYTE] = 0x55;
                pBufResp[A_RESP_NUMB_L] = (uint8_t)(size_resp_packet & 0xFF);
                pBufResp[A_RESP_NUMB_H] = (uint8_t)((size_resp_packet >> 8) & 0xFF);
                pBufResp[A_RESP_COMMAND_WORD] = (uint8_t) COMMAND_START_RECORD;
                pBufResp[A_RESP_ADDRESS_L] = 0;
                pBufResp[A_RESP_ADDRESS_H] = 0;
                pBufResp[A_RESP_NUMW_L] = (uint8_t)(record_num_addrs & 0xFF);
                pBufResp[A_RESP_NUMW_H] = (uint8_t)((record_num_addrs >> 8) & 0xFF);

                // Checksum
                uint8_t checksum = 0;
                for (int i = 0; i < size_resp_packet - 1; i++) checksum += pBufResp[i];
                pBufResp[size_resp_packet - 1] = checksum;

                // Write directly to USB CDC (bypass queue, no logging)
                tinyusb_cdcacm_write_queue(TINYUSB_CDC_ACM_0, pBufResp, size_resp_packet);
                tinyusb_cdcacm_write_flush(TINYUSB_CDC_ACM_0, 2);
            }

            xEventGroupSetBits(event_group_record, 2);
        }

        vTaskDelete(NULL);
    }


    static void main_task(void *pvParameters)
    {
        CommandHandler* obj = reinterpret_cast<CommandHandler*>(pvParameters);
        uint8_t* pBufResp = (uint8_t*) malloc(idf::WR_BUF_SIZE);

        uartToMainQueueItem_t req;
        spi_transaction_t t;
        memset(&t, 0, sizeof(t));
        uartToMainQueueItem_t itemRespQueue;
       const TickType_t vppDelayMs = pdMS_TO_TICKS(200);
       uint16_t argument2;
        for(;;) {
            xQueueReceive(obj->parsingPacket_queue, &req, portMAX_DELAY);   
            bzero(pBufResp, WR_BUF_SIZE);
            int size_resp_packet = 0;
            if(check_request(req) == (int)-1){  
                pBufResp[idf::A_RESP_STARTBYTE] = (uint8_t)0x55;
                pBufResp[A_RESP_NUMB_L] = (uint8_t)0x05;
                pBufResp[A_RESP_NUMB_H] = (uint8_t)0x00;
                pBufResp[A_RESP_COMMAND_WORD] = (uint8_t)0xFF;
                pBufResp[A_RESP_COMMAND_WORD+1] = (uint8_t)0x00;
                 size_resp_packet = 384*2 + 9;
                itemRespQueue.pbuf = pBufResp;
                itemRespQueue.len = (uint16_t) size_resp_packet;
                xQueueSend(obj->uart_resp_queue, &itemRespQueue, portMAX_DELAY);
                continue;
            }
            uint8_t commandByte =  (uint8_t) * (req.pbuf+A_REQ_COMMAND_WORD);
            pBufResp[0] = RESP_START_BYTE;
            pBufResp[3] = commandByte;
            switch (commandByte){
                case COMMAND_STOP_READ_CYCLIC:

                break;
                case COMMAND_STOP_RECORD:
                    if(recordModeIsEna == 1){
                        xEventGroupClearBits(event_group_record, 1);
                        EventBits_t bits = xEventGroupWaitBits(event_group_record, 2, pdFALSE, pdTRUE, portMAX_DELAY);
                        recordModeIsEna = 0;
                        if(record_addr_buffer != nullptr){
                            free(record_addr_buffer);
                            record_addr_buffer = nullptr;
                        }
                    }
                    size_resp_packet = 9;
                    pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                    pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                break;
                default:
                    uint16_t address = ((uint8_t) *(req.pbuf+A_REQ_ADDRESS_H )  << 8 ) + (uint8_t) * ( req.pbuf+A_REQ_ADDRESS_L  );
                    uint16_t size    = ((uint8_t) *(req.pbuf+A_REQ_LEN_H)  << 8 ) + (uint8_t) * ( req.pbuf+A_REQ_LEN_L );
                    pBufResp[A_RESP_ADDRESS_L]   = *(req.pbuf+A_REQ_ADDRESS_L);
                    pBufResp[A_RESP_ADDRESS_H]   = *(req.pbuf+A_REQ_ADDRESS_H);
                    pBufResp[A_RESP_NUMW_L]      = *(req.pbuf+A_REQ_LEN_L);
                    pBufResp[A_RESP_NUMW_H]      = *(req.pbuf+A_REQ_LEN_H);
                    switch (commandByte) {
                        case COMMAND_READ:
                            ESP_LOGD(TAG, "COMMAND_READ");
                            obj->spim.readWordsFromIc(address, size, &pBufResp[A_RESP_VALUE0_L], &t);
                                size_resp_packet = size*2 + 9;
                                pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                                pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_WRITE:
                            obj->spim.writeWordsToIc(address, size, &pBufResp[A_RESP_VALUE0_L], &t , req.pbuf+A_REQ_VALUE0_L);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_RANDOM_READ:
                            obj->spim.readRandomAddressValues( size, &pBufResp[A_RESP_VALUE0_L], &t, req.pbuf+A_REQ_VALUE0_L);
                            size_resp_packet = size*2 + 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_WRITE_RANDOM:
                            obj->spim.writeRandomWordsToIC(address, size, &pBufResp[A_RESP_VALUE0_L], &t , req.pbuf+A_REQ_VALUE0_L);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_WRITE_BOTP:
                            obj->spim.writeRandomWordsToIC(address, size, &pBufResp[A_RESP_VALUE0_L], &t , req.pbuf+A_REQ_VALUE0_L);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                            itemRespQueue.pbuf = pBufResp;
                            itemRespQueue.len = (uint16_t) size_resp_packet;
                            xQueueSend(obj->uart_resp_queue, &itemRespQueue, portMAX_DELAY);
                            gpio_set_level((gpio_num_t) VPP9V_PIN, vpp9v_level(0));
                            vTaskDelay(vppDelayMs);
                            gpio_set_level((gpio_num_t) VPP9V_PIN, vpp9v_level(1));
                            continue;
                        break;
                        case COMMAND_READ_CPU_REGS:
                            ESP_LOGD(TAG, "COMMAND_READ_CPU_REGS");
                            obj->readCpuRegs(address, size, &pBufResp[A_RESP_VALUE0_L], &t);
                            pBufResp[A_RESP_NUMW_L]      = 26;
                            pBufResp[A_RESP_NUMW_H]      = 0;
                            size_resp_packet = 9 + 4*13;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_SET_LED:
                            gpio_set_level((gpio_num_t)LED_PIN, address);
                            milandrMode = address;
                            obj->spim.setMilandrMode(milandrMode);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_SET_STNDBY:
                            gpio_set_level((gpio_num_t) STNDBY_PIN, address);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_SET_VPP9V:

                            gpio_set_level((gpio_num_t) VPP9V_PIN, vpp9v_level(address ^ 1));
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_SET_NRESET:
                            ESP_LOGD(TAG, "COMMAND_SET_NRESET");
                            gpio_set_level((gpio_num_t) GPIO_NRESET, address);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_SET_SPI_SPEED:
                        {
                            uint16_t speed = (uint16_t)  address;
                            ESP_LOGD(TAG, "COMMAND_SET_SPI_SPEED: speed = %d", address);
                            obj->spim.setClockSpeed(speed * 1000);
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        }
                        break;
                        case COMMAND_SET_DMA_QUAD:
                            argument2 = (uint16_t) address;
                            ESP_LOGD(TAG, "COMMAND_SET_DMA_QUAD, argument2=%d, size=%d",argument2, size);

                            if(argument2 == 0){
                                // STOP: release DMA SPI, recapture regular SPI
                                if(dmaQuadModeIsEna == 1){
                                    gpio_reset_pin((gpio_num_t) GPIO_MOSI);
                                    obj->masterDmaSpi.releaseSpiBus();
                                    vTaskDelay(pdMS_TO_TICKS(10));
                                    obj->spim.captureSpiBus();
                                    dmaQuadModeIsEna = 0;
                                    // Drain any stale DMA data from the response queue
                                    {
                                        idf::uartToMainQueueItem_t stale;
                                        while(xQueueReceive(obj->uart_resp_queue, &stale, 0) == pdTRUE) {
                                            ESP_LOGI(TAG, "DMA Quad stop: drained stale queue item (len=%d)", stale.len);
                                        }
                                    }
                                    continue;
                                }
                            } else if(argument2 == 2) {
                                // TRIGGER: do SPI transfer directly in main task, send response
                                if(dmaQuadModeIsEna == 1){
                                    static constexpr size_t DMA_QUAD_BUF_SIZE = 4096;
                                    size_resp_packet = 9 + DMA_QUAD_BUF_SIZE;

                                    // Prepare response header
                                    pBufResp[idf::A_RESP_STARTBYTE] = (uint8_t)0x55;
                                    pBufResp[A_RESP_NUMB_L] = (uint8_t)(size_resp_packet & 0xFF);
                                    pBufResp[A_RESP_NUMB_H] = (uint8_t)((size_resp_packet >> 8) & 0xFF);
                                    pBufResp[A_RESP_COMMAND_WORD] = (uint8_t)0xFF;
                                    pBufResp[A_RESP_COMMAND_WORD+1] = (uint8_t)0x00;
                                    pBufResp[A_RESP_ADDRESS_L]   = (uint8_t)0x00;
                                    pBufResp[A_RESP_ADDRESS_H]   = (uint8_t)0x00;
                                    pBufResp[A_RESP_NUMW_L]      = (uint8_t)((DMA_QUAD_BUF_SIZE>>1)& 0xFF);
                                    pBufResp[A_RESP_NUMW_H]      = (uint8_t)((DMA_QUAD_BUF_SIZE>>(1+8))& 0xFF);

                                    // SPI transfer directly — no separate task needed
                                    obj->masterDmaSpi.transferPolling(SPI_TRANS_MODE_QIO, dma_tx_buf, &pBufResp[A_RESP_VALUE0_L], DMA_QUAD_BUF_SIZE);

                                    // Falls through to break → generic xQueueSend at end of main loop
                                } else {
                                    continue;
                                }
                            } else {
                                // ENABLE (argument2 == 1): setup SPI bus for DMA Quad
                                if(dmaQuadModeIsEna == 0){
                                    // Drain any stale items from the response queue before starting
                                    {
                                        idf::uartToMainQueueItem_t stale;
                                        while(xQueueReceive(obj->uart_resp_queue, &stale, 0) == pdTRUE) {
                                            ESP_LOGI(TAG, "DMA Quad enable: drained stale queue item (len=%d)", stale.len);
                                        }
                                    }
                                    // Write Addr1 and Addr2 to IC registers 760 and 762
                                    if(size >= 2) {
                                        uint8_t* addrData = req.pbuf + A_REQ_VALUE0_L;
                                        uint16_t addr1_val = (uint16_t)(addrData[1] << 8) | addrData[0];
                                        uint16_t addr2_val = (uint16_t)(addrData[3] << 8) | addrData[2];
                                        ESP_LOGI(TAG, "Write Addr1=%d to IC[760], Addr2=%d to IC[762]", addr1_val, addr2_val);
                                        obj->spim.writeWordsToIc(760, 1, pBufResp, &t, addrData);
                                        obj->spim.writeWordsToIc(762, 1, pBufResp, &t, addrData + 2);
                                    }
                                    obj->spim.releaseSpiBus();
                                    vTaskDelay(pdMS_TO_TICKS(10));
                                    bool useEnc2 = (sdiValueForDmaQuad != 0);
                                    obj->masterDmaSpi.captureSpiBusDmaQuad(useEnc2);
                                    vTaskDelay(pdMS_TO_TICKS(10));
                                    gpio_set_direction  ((gpio_num_t)GPIO_MOSI, GPIO_MODE_OUTPUT);
                                    gpio_set_level      ((gpio_num_t)GPIO_MOSI, sdiValueForDmaQuad);
                                    size_resp_packet = 9;
                                    pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                                    pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                                    ESP_LOGI(TAG, "DMA Quad enabled");
                                    dmaQuadModeIsEna = 1;
                                }
                            }

                        break;
                        case COMMAND_SET_SDI:
                            sdiValueForDmaQuad = 0x01 & address;
                            if(dmaQuadModeIsEna == 1){
                                // SDI changed: reconfigure SPI bus to read from ENC1 or ENC2 pins
                                gpio_reset_pin((gpio_num_t)GPIO_MOSI);
                                obj->masterDmaSpi.releaseSpiBus();
                                vTaskDelay(pdMS_TO_TICKS(10));
                                bool useEnc2_sdi = (sdiValueForDmaQuad != 0);
                                obj->masterDmaSpi.captureSpiBusDmaQuad(useEnc2_sdi);
                                vTaskDelay(pdMS_TO_TICKS(10));
                                gpio_set_direction((gpio_num_t)GPIO_MOSI, GPIO_MODE_OUTPUT);
                                gpio_set_level((gpio_num_t)GPIO_MOSI, sdiValueForDmaQuad);
                                ESP_LOGI(TAG, "SDI changed to %d → reconfigured SPI to %s pins",
                                         sdiValueForDmaQuad, useEnc2_sdi ? "ENC2" : "ENC1");
                            }
                            continue;
                        case COMMAND_SET_VC:
                            vcValueForDmaQuad = 0x01 & address;
                            gpio_set_level((gpio_num_t) GPIO_VC, vcValueForDmaQuad);
                            if(dmaQuadModeIsEna == 1){
                                // VC changed: reconfigure SPI bus (VC may affect output routing)
                                gpio_reset_pin((gpio_num_t)GPIO_MOSI);
                                obj->masterDmaSpi.releaseSpiBus();
                                vTaskDelay(pdMS_TO_TICKS(10));
                                bool useEnc2_vc = (sdiValueForDmaQuad != 0);
                                obj->masterDmaSpi.captureSpiBusDmaQuad(useEnc2_vc);
                                vTaskDelay(pdMS_TO_TICKS(10));
                                gpio_set_direction((gpio_num_t)GPIO_MOSI, GPIO_MODE_OUTPUT);
                                gpio_set_level((gpio_num_t)GPIO_MOSI, sdiValueForDmaQuad);
                                ESP_LOGI(TAG, "VC changed to %d → reconfigured SPI (useEnc2=%d)",
                                         vcValueForDmaQuad, useEnc2_vc);
                            }
                            continue;
                        break;
                        case COMMAND_SET_MASTER_FPGA_MODE:
                            fpgaMasterMode = address;
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        
                        case COMMAND_READ_HANDTAP:
                            obj->readHandTapValues(address, size, &pBufResp[A_RESP_VALUE0_L], &t, req.pbuf+A_REQ_VALUE0_L );
                            size_resp_packet = 384*2 + 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                            pBufResp[A_RESP_ADDRESS_L]   =  (uint8_t) (0x07 & (*(req.pbuf+A_REQ_VALUE0_L+2)));
                            pBufResp[A_RESP_ADDRESS_H]   =  (uint8_t)0;
                            pBufResp[A_RESP_NUMW_L]      =  (uint8_t)0x80;
                            pBufResp[A_RESP_NUMW_H]      =  (uint8_t)1;
                        break;
                        case COMMAND_SET_DMA_SINGLE:
                            argument2 = (uint16_t) 0x01 & address;
                            ESP_LOGD(TAG, "COMMAND_SET_DMA_SINGLE, argument2=%d",argument2);
                            if(argument2 == 0){
                                if(dmaSingleModeIsEna == 1){
                                    xEventGroupClearBits(event_group_dma_single, 1);
                                    EventBits_t bits = xEventGroupWaitBits(event_group_dma_single, 2, pdFALSE,pdTRUE, portMAX_DELAY);
                                    obj->masterDmaSpi.releaseSpiBus();
                                    obj->spim.captureSpiBus();
                                    dmaSingleModeIsEna = 0;
                                    continue;
                                }
                            } else {
                                if(dmaSingleModeIsEna == 0){
                                    sizePbufDmaSingleBuffer = size;
                                    pbufDmaSingleBuffer = req.pbuf + A_REQ_VALUE0_L;
                                    obj->spim.releaseSpiBus();
                                    obj->masterDmaSpi.captureSpiBusDmaSingle();
                                    xEventGroupSetBits(event_group_dma_single, 1);
                                    size_resp_packet = 9;
                                    pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                                    pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                                    dmaSingleModeIsEna = 1;
                                    continue;
                                }
                            }
                        break;
                        case COMMAND_ENC_ON:
                            ESP_LOGD(TAG, "COMMAND_ENC_ON: enabling encoders");
                            encoderManager->enable();
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_ENC_OFF:
                            ESP_LOGD(TAG, "COMMAND_ENC_OFF: disabling encoders");
                            encoderManager->disable();
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        break;
                        case COMMAND_READ_ENCODERS:
                        {
                            int32_t enc1Count = encoderManager->getEnc1Count();
                            int32_t enc2Count = encoderManager->getEnc2Count();
                            ESP_LOGD(TAG, "COMMAND_READ_ENCODERS: ENC1=%d, ENC2=%d", enc1Count, enc2Count);
                            
                            // Pack encoder counts into response (2 int32_t values = 8 bytes)
                            pBufResp[A_RESP_VALUE0_L + 0] = (uint8_t) (enc1Count & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 1] = (uint8_t) ((enc1Count >> 8) & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 2] = (uint8_t) ((enc1Count >> 16) & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 3] = (uint8_t) ((enc1Count >> 24) & 0xFF);

                            pBufResp[A_RESP_VALUE0_L + 4] = (uint8_t) (enc2Count & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 5] = (uint8_t) ((enc2Count >> 8) & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 6] = (uint8_t) ((enc2Count >> 16) & 0xFF);
                            pBufResp[A_RESP_VALUE0_L + 7] = (uint8_t) ((enc2Count >> 24) & 0xFF);

                            size_resp_packet = 8 + 9;  // 8 bytes data + 9 bytes header
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                            pBufResp[A_RESP_NUMW_L] = 4;  // 2 words (4 bytes per word)
                            pBufResp[A_RESP_NUMW_H] = 0;
                        }
                        break;
                        case COMMAND_START_RECORD:
                        {
                            ESP_LOGD(TAG, "COMMAND_START_RECORD: size=%d", size);
                            // SPI pipeline: 1-я транзакция - dummy (priming),
                            // со 2-й получаем данные предыдущего адреса.
                            // Для N адресов нужно N+1 элементов в буфере:
                            // [A0, A1, ..., A(N-1), A0]
                            // Тогда rValues[0..N-1] = данные для A0..A(N-1)
                            record_num_addrs = size;
                            if(record_addr_buffer != nullptr){
                                free(record_addr_buffer);
                            }
                            record_addr_buffer = (uint8_t*) malloc((size + 1) * 2);
                            // Копируем все адреса из payload
                            memcpy(record_addr_buffer, req.pbuf + A_REQ_VALUE0_L, size * 2);
                            // Добавляем первый адрес в конец - чтобы вывести данные последнего
                            record_addr_buffer[size * 2]     = *(req.pbuf + A_REQ_VALUE0_L);
                            record_addr_buffer[size * 2 + 1] = *(req.pbuf + A_REQ_VALUE0_L + 1);

                            // Убеждаемся что SPI bus не занят DMA
                            if(dmaQuadModeIsEna == 1 || dmaSingleModeIsEna == 1){
                                ESP_LOGI(TAG, "Cannot start record: DMA mode is active");
                                size_resp_packet = 9;
                                pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                                pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                                break;
                            }

                            xEventGroupSetBits(event_group_record, 1);
                            recordModeIsEna = 1;
                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t) ( size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t) ((size_resp_packet >> 8) & 0xFF);
                        }
                        break;

                        case COMMAND_SET_ELEC_OFFSET:
                        {
                            int16_t offset_x100 = (int16_t)(*(req.pbuf + A_REQ_VALUE0_L) |
                                                           (*(req.pbuf + A_REQ_VALUE0_H) << 8));
                            ESP_LOGD(TAG, "COMMAND_SET_ELEC_OFFSET raw=%d (no motor)", offset_x100);

                            size_resp_packet = 9;
                            pBufResp[A_RESP_NUMB_L] = (uint8_t)(size_resp_packet & 0xFF);
                            pBufResp[A_RESP_NUMB_H] = (uint8_t)((size_resp_packet >> 8) & 0xFF);
                        }
                        break;
                    }

            }
            itemRespQueue.pbuf = pBufResp;
            itemRespQueue.len = (uint16_t) size_resp_packet;
             if(xQueueSend(obj->uart_resp_queue, &itemRespQueue, pdMS_TO_TICKS(500)) != pdTRUE) {
                 ESP_LOGE(TAG, "xQueueSend TIMEOUT — response queue full, dropping response");
             }
        }
       
        vTaskDelete(NULL);

    }

    private:

 

};


}
