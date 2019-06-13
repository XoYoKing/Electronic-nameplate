#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>
#include <fcntl.h>
#include <termios.h>

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>

#include <linux/types.h>
#include <linux/spi/spidev.h>
//#include <include/linux/spi/spi.h>

#include <jni.h>

//#include "spidev.h"
//#include "com_example_tcp_spiDevice.h"
//#include "ra8876l.h"
#include "com_itc_ts8209a_drive_Ra8876l.h"

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))

#include "android/log.h"
static const char *TAG = "RA8876L";

#define 	log   	1
//#define 	debug 	0
//#define     info    1
//#define     err     1

#if log

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

#else

#define LOGI(fmt, args...) 
#define LOGD(fmt, args...)
#define LOGE(fmt, args...) 

#endif

//#define 	ColorDepth_16bit
#define 	ColorDepth_24bit

#define TIMEOUT_WARNNING 1


#define OSC_FREQ	    10	// OSC clock frequency, unit: MHz.
#define DRAM_FREQ	    133	// SDRAM clock frequency, unit: MHz.
#define CORE_FREQ	    120	// Core (system) clock frequency, unit: MHz.
#define SCAN_FREQ	    50	// Panel Scan clock frequency, unit: MHz.
#define Panel_width     1024
#define Panel_length    600
#define canvus_width    1024

#define	cSetb0		0x01
#define	cSetb1		0x02
#define	cSetb2		0x04
#define	cSetb3		0x08
#define	cSetb4		0x10
#define	cSetb5		0x20
#define	cSetb6		0x40
#define	cSetb7		0x80

#define	cClrb0		0xfe
#define	cClrb1		0xfd
#define	cClrb2		0xfb
#define	cClrb3		0xf7
#define	cClrb4		0xef
#define	cClrb5		0xdf
#define	cClrb6		0xbf
#define	cClrb7		0x7f

#define color16M_red     0x00ff0000

#define WaitTime 30

//#define PackSize 230400
//#define PackSize 921600
#define PackSize 1843200

static const char *device = "/dev/spidev0.0";
static uint8_t mode;
static uint8_t bits = 8;
static uint32_t speed = 200000;
static uint16_t delay = 0;

int dev;

#ifdef __cplusplus
extern "C" {
#endif

/*********************************************** SPI ****************************************************/
void SPI_Config(uint32_t spd) {
	int ret = 0;

//		mode |= SPI_LOOP;
	mode |= SPI_CPHA;
	mode |= SPI_CPOL;
//		mode |= SPI_LSB_FIRST;
//		mode |= SPI_CS_HIGH;
//		mode |= SPI_3WIRE;
//		mode |= SPI_NO_CS;
//		mode |= SPI_READY;

	if (dev < 0)
		LOGE("can't open device");

	LOGI("start spi config");
	/*
	 * spi mode
	 */
	ret = ioctl(dev, SPI_IOC_WR_MODE, &mode);
	if (ret == -1)
		LOGE("can't set spi mode");

	LOGI("set spi mode finish");

	ret = ioctl(dev, SPI_IOC_RD_MODE, &mode);
	if (ret == -1)
		LOGE("can't get spi mode");

	/*
	 * bits per word
	 */
	ret = ioctl(dev, SPI_IOC_WR_BITS_PER_WORD, &bits);
	if (ret == -1)
		LOGE("can't set bits per word");

	ret = ioctl(dev, SPI_IOC_RD_BITS_PER_WORD, &bits);
	if (ret == -1)
		LOGE("can't get bits per word");

	/*
	 * max speed hz
	 */
	speed = spd;

	ret = ioctl(dev, SPI_IOC_WR_MAX_SPEED_HZ, &speed);
	if (ret == -1)
		LOGE("can't set max speed hz");

	ret = ioctl(dev, SPI_IOC_RD_MAX_SPEED_HZ, &speed);
	if (ret == -1)
		LOGE("can't get max speed hz");

	LOGD("spi device: %s\n", device);
	LOGD("spi mode: %d\n", mode);
	LOGD("bits per word: %d\n", bits);
	LOGD("delay: %d\n", delay);
	LOGD("max speed: %d Hz (%d %s)\n",
			speed, ((speed/1000) > 1000 ? speed/1000000 : speed/1000), ((speed/1000) > 1000 ? "MHz" : "KHz"));

}

static uint8_t ReadWriteByte(uint8_t *data, uint8_t len) {
	int ret;
	int status;
	uint8_t i;

	uint8_t sbuf[len], rbuf[len];

	struct spi_ioc_transfer xfer;

	memset(sbuf, 0, len);
	memset(rbuf, 0, len);
	memcpy(sbuf, data, len);

	xfer.tx_buf = (unsigned long) sbuf;
	xfer.rx_buf = (unsigned long) rbuf;
	xfer.len = len;
	xfer.delay_usecs = delay;
	xfer.speed_hz = speed;
	xfer.bits_per_word = bits;

	ret = ioctl(dev, SPI_IOC_MESSAGE(1), &xfer);

	if (ret < 1)
		LOGE("%s ;can't send spi message", __FUNCTION__);

	/*	for (i = 0; i < len; i++) {
	 LOGD("s:%.2X r:%.2X", sbuf[i], rbuf[i]);
	 }*/

	return rbuf[len - 1];
}

#define	bufLen	92161
#define	dataLen	(bufLen - 1)

void WriteOnlyDataArr(uint8_t *data, uint32_t len, uint8_t cs) {
	int ret;
	uint32_t i;
	uint16_t bufRemain = len % dataLen;
	uint32_t bufNum = (bufRemain == 0) ? len / dataLen : len / dataLen + 1;
//	uint8_t		txbuf[bufNum][bufLen];
	uint8_t *txbuf[bufNum];

//	struct spi_ioc_transfer xfer[bufNum];

//	memset(xfer, 0, sizeof xfer);
//	memset(txbuf, 0,sizeof txbuf);

	LOGI("bufNum = %d,bufRemain = %d", bufNum, bufRemain);

	LOGI("SPI_IOC_MESSAGE");
	for (i = 0; i < bufNum; i++) {
		txbuf[i] = malloc(bufLen * (sizeof(uint8_t)));
		txbuf[i][0] = 0x80;
		if ((i == bufNum - 1) && (bufRemain != 0)) {
//			LOGI("doing bufRemain %d",i);
			memcpy(txbuf[i] + 1, data + i * dataLen, bufRemain);

			write(dev, txbuf[i], bufRemain + 1);

		} else {
//			LOGI("doing bufNum %d",i);
			memcpy(txbuf[i] + 1, data + i * dataLen, dataLen);

			write(dev, txbuf[i], bufLen);

		}
	}

//	ret = ioctl(dev, SPI_IOC_MESSAGE(bufNum), xfer);
//	LOGI("sizeof(xfer) = %d,sizeof(struct spi_ioc_transfer) = %d",sizeof(xfer),sizeof(struct spi_ioc_transfer));
//	ret = write(dev, xfer, bufNum*sizeof(struct spi_ioc_transfer));
	LOGI("SPI_IOC_MESSAGE_FINISH");
	if (ret < 1)
		LOGE("%s ;can't send spi message", __FUNCTION__);

	for (i = 0; i < bufNum; i++) {
		free(txbuf[i]);
	}
}

static uint8_t StatusRead(void) {
	uint8_t Data, rw[2] = { 0x40, 0xFF };

	Data = ReadWriteByte(rw, 2);
	return Data;
}

static void Check_Busy(void) {
	uint16_t i;
	for (i = 0; i < WaitTime; i++) {
		if ((StatusRead() & 0x08) == 0x00) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
		}


	if (i >= WaitTime) {
		LOGE("Check_Busy fail");
	}
//		do{	}
//		while(( StatusRead()&0x08)==0x08 );
}

static void CmdWrite(uint8_t cmd) {
	uint8_t rw[2] = { 0x00, cmd };

	ReadWriteByte(rw, 2);
}

static void DataWrite(uint8_t Data) {
	uint8_t rw[2] = { 0x80, Data };

	ReadWriteByte(rw, 2);
}

static uint8_t DataRead(void) {
	uint8_t Data, rw[2] = { 0xC0, 0xFF };

	Data = ReadWriteByte(rw, 2);

//  ReadWriteByte(0XFF);
	return Data;
}

static void RegisterWrite(uint8_t Cmd, uint8_t Data) {
	Check_Busy();
	usleep(1);
	CmdWrite(Cmd);
	DataWrite(Data);
}

static void Check_SDRAM_Ready(void) {
	/*	[Status Register] bit2
	 SDRAM ready for access
	 0: SDRAM is not ready for access
	 1: SDRAM is ready for access
	 */
	uint16_t i;
	for (i = 0; i < WaitTime; i++) {
		if ((StatusRead() & 0x04) == 0x04) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}

	if (i >= WaitTime) {
		LOGE("Check_SDRAM_Ready fail");
	}

	/* or the following */
//	do{	}
//	while( (StatusRead()&0x04) == 0x00 );
}

static void SDRAM_init(void) {
	uint8_t CAS_Latency;
	uint32_t Auto_Refresh;

	LOGI("SDRAM_init W9864G6 start");

	if (DRAM_FREQ <= 133)
		CAS_Latency = 2;
	else
		CAS_Latency = 3;

	Auto_Refresh = (64 * DRAM_FREQ * 1000) / (4096);

	RegisterWrite(0xe0, 0x28);
	RegisterWrite(0xe1, CAS_Latency); //CAS:2=0x02顡嘋AS:3=0x03
	RegisterWrite(0xe2, Auto_Refresh);
	RegisterWrite(0xe3, Auto_Refresh >> 8);
	RegisterWrite(0xe4, 0x01);
	/*
	 LOGI("SDRAM_init W9825G6JH start");
	 CAS_Latency=3;
	 Auto_Refresh=(64*DRAM_FREQ*1000)/(8192);

	 RegisterWrite(0xe0,0x31);
	 RegisterWrite(0xe1,CAS_Latency);      //CAS:2=0x02顡嘋AS:3=0x03
	 RegisterWrite(0xe2,Auto_Refresh);
	 RegisterWrite(0xe3,Auto_Refresh>>8);
	 RegisterWrite(0xe4,0x01);*/

	Check_SDRAM_Ready();
}

static void Enable_PLL(void) {
	/*  0: PLL disable; allow change PLL parameter.
	 1: PLL enable; cannot change PLL parameter.*/
	uint8_t temp;
	uint16_t i;

	CmdWrite(0x01);
	temp = DataRead();
	temp |= cSetb7;
	DataWrite(temp);

	usleep(10);
//	LOGI("Enable_PLL start");
	for (i = 0; i < 100; i++) {
		CmdWrite(0x01);
		temp = DataRead();
		if ((temp & 0x80) == 0x80) {
			break;
		} else {
			LOGD("temp = %.2X", temp);
		}
	}

	if (i >= 100) {
		LOGE("Enable_PLL fail ");
	}
}

static void PLL_init(void) {
//	uint16_t x_Divide,PLLC1,PLLC2;
//	uint16_t pll_m_lo, pll_m_hi;
//	uint8_t temp;
	LOGI("PLL_init start");

	RegisterWrite(0x05, 0x04); //PLL Divided by 4
	RegisterWrite(0x06, (SCAN_FREQ * 4 / OSC_FREQ) - 1);

	RegisterWrite(0x07, 0x02); //PLL Divided by 2
	RegisterWrite(0x08, (DRAM_FREQ * 2 / OSC_FREQ) - 1);

	RegisterWrite(0x09, 0x02); //PLL Divided by 2
	RegisterWrite(0x0A, (CORE_FREQ * 2 / OSC_FREQ) - 1);

	Enable_PLL();
	usleep(100 * 1000);
}

static void System_Check(void) {
	uint8_t i = 0;
	uint8_t temp = 0;
	uint8_t system_ok = 0;
	LOGI("System_Check start");
	do {
		if ((StatusRead() & 0x02) == 0x00) {
			usleep(1000);
			CmdWrite(0x01);
			usleep(1000);
			temp = DataRead();
			if ((temp & 0x80) == 0x80) {
				system_ok = 1;
				i = 0;
			} else {
				usleep(1000); //If MCU interface too fast, maybe need some delay time.//鐠滿CU 纭夘倲銇承殿搰鎯犵Ц淇扁攽绛愵啝涓侇搲
				CmdWrite(0x01);
				usleep(1000);
				DataWrite(0x80);
			}
		} else {
			system_ok = 0;
			i++;
		}
		if (system_ok == 0 && i >= 5) {
			LOGE("System_Check fail :Status=%x", StatusRead());
			break;
		}
	} while (system_ok == 0);
}

static void Check_IC(void) {
	uint16_t i;

	LOGI("Check_IC start");

	for (i = 0; i < WaitTime; i++) {
		if ((StatusRead() & 0x02) == 0x00) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}

	if (i >= WaitTime) {
		LOGE("Check_IC fail ");
	}
}

static void TFT_24bit(void) //RA8876 only
{
	uint8_t temp;

	CmdWrite(0x01);
	temp = DataRead();
	temp &= cClrb4; //cClrb4
	temp &= cClrb3; //cClrb3
	DataWrite(temp);
}

static void TFT_16bit(void) //RA8876 only
{
	uint8_t temp;
	CmdWrite(0x01);
	temp = DataRead();
	temp |= cSetb4;
	temp &= cClrb3;
	DataWrite(temp);
}
static void Host_Bus_8bit(void) {
	uint8_t temp;

	CmdWrite(0x01);
	temp = DataRead();
	temp &= cClrb0; //cClrb0
	DataWrite(temp);
}

static void Data_Format_8b_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x02);
	temp = DataRead();
	temp &= cClrb7;
	DataWrite(temp);
}

void Data_Format_8b_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x02);
	temp = DataRead();
	temp &= cClrb7;
	DataWrite(temp);
}

static void MemWrite_Left_Right_Top_Down(void) {
	uint8_t temp;

	CmdWrite(0x02);
	temp = DataRead();
	temp &= cClrb2;
	temp &= cClrb1;
	DataWrite(temp);
}

void MemWrite_Right_Left_Top_Down(void) {
	uint8_t temp;
	CmdWrite(0x02);
	temp = DataRead();
	temp &= cClrb2;
	temp |= cSetb1;
	DataWrite(temp);
}

void MemWrite_Down_Top_Left_Right(void) {
	uint8_t temp;
	CmdWrite(0x02);
	temp = DataRead();
	temp |= cSetb2;
	temp |= cSetb1;
	DataWrite(temp);
}

static void Graphic_Mode(void) {
	uint8_t temp;

	CmdWrite(0x03);
	temp = DataRead();
	temp &= cClrb2;
	DataWrite(temp);
}

static void Memory_Select_SDRAM(void) {
	uint8_t temp;

	CmdWrite(0x03);
	temp = DataRead();
	temp &= cClrb1;
	temp &= cClrb0;
	DataWrite(temp);

}

/************************* 24bit config  ***************************/
static void Select_Main_Window_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x10);
	temp = DataRead();
	temp |= cSetb3;
	DataWrite(temp);
}

static void Memory_24bpp_Mode(void) {
	uint8_t temp;

	CmdWrite(0x5E);
	temp = DataRead();
	temp |= cSetb1;
//	temp |= cSetb0;
	DataWrite(temp);
}

static void Select_PIP1_Window_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x11);
	temp = DataRead();
	temp |= cSetb3;
//    temp &= cClrb2;
	DataWrite(temp);
}

static void Select_PIP2_Window_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x11);
	temp = DataRead();
	temp |= cSetb1;
//    temp &= cClrb0;
	DataWrite(temp);
}

static void BTE_S0_Color_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x92);
	temp = DataRead();
	temp |= cSetb6;
	//temp |= cSetb5 ;
	DataWrite(temp);
}

static void BTE_S1_Color_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x92);
	temp = DataRead();
	temp &= cClrb4;
	temp |= cSetb3;
	temp &= cClrb2;
	DataWrite(temp);
}

static void BTE_Destination_Color_24bpp(void) {
	uint8_t temp;

	CmdWrite(0x92);
	temp = DataRead();
	temp |= cSetb1;
	//temp |= cSetb0 ;
	DataWrite(temp);
}

/************************* 16bit config  ***************************/
void Select_Main_Window_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x10);
	temp = DataRead();
	temp &= cClrb3;
	temp |= cSetb2;
	DataWrite(temp);
}

void Memory_16bpp_Mode(void) {
	uint8_t temp;

	CmdWrite(0x5E);
	temp = DataRead();
	temp &= cClrb1;
	temp |= cSetb0;
	DataWrite(temp);
}

void Select_PIP1_Window_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x11);
	temp = DataRead();
	temp &= cClrb3;
	temp |= cSetb2;
	DataWrite(temp);
}

void Select_PIP2_Window_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x11);
	temp = DataRead();
	temp &= cClrb1;
	temp |= cSetb0;
	DataWrite(temp);
}

void BTE_S0_Color_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x92);
	temp = DataRead();
	temp &= cClrb6;
	temp |= cSetb5;
	DataWrite(temp);

}

void BTE_S1_Color_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x92);
	temp = DataRead();
	temp &= cClrb4;
	temp &= cClrb3;
	temp |= cSetb2;
	DataWrite(temp);

}

void BTE_Destination_Color_16bpp(void) {
	uint8_t temp;
	CmdWrite(0x92);
	temp = DataRead();
	temp &= cClrb1;
	temp |= cSetb0;
	DataWrite(temp);

}
/*******************Set_LCD_Panel*******************************/
static void Select_LCD_Sync_Mode(void) {
	uint8_t temp;
	CmdWrite(0x10);
	temp = DataRead();
	temp &= cClrb0;
	DataWrite(temp);
}

static void PCLK_Falling(void) {
	uint8_t temp;
	CmdWrite(0x12);
	temp = DataRead();
	temp |= cSetb7;
	DataWrite(temp);
}

static void VSCAN_T_to_B(void) {
	uint8_t temp;

	CmdWrite(0x12);
	temp = DataRead();
	temp &= cClrb3;
	DataWrite(temp);
}

static void PDATA_Set_BGR(void) {
	uint8_t temp;

	CmdWrite(0x12);
	temp = DataRead();
	temp &= 0xf8;
	temp |= cSetb2;
	temp |= cSetb0;
	DataWrite(temp);
}

void PDATA_Set_RGB(void) {
	uint8_t temp;

	CmdWrite(0x12);
	temp = DataRead();
	temp &= 0xf8;
	DataWrite(temp);
}

static void HSYNC_Low_Active(void) {
	uint8_t temp;

	CmdWrite(0x13);
	temp = DataRead();
	temp &= cClrb7;
	DataWrite(temp);
}

static void VSYNC_Low_Active(void) {
	uint8_t temp;

	CmdWrite(0x13);
	temp = DataRead();
	temp &= cClrb6;
	DataWrite(temp);
}

void HSYNC_High_Active(void)
{
    uint8_t temp;

    CmdWrite(0x13);
    temp = DataRead();
    temp |= cSetb7;
    DataWrite(temp);
}

void VSYNC_High_Active(void)
{
    uint8_t temp;

    CmdWrite(0x13);
    temp = DataRead();
    temp |= cSetb6;
    DataWrite(temp);
}

static void DE_High_Active(void) {
	uint8_t temp;

	CmdWrite(0x13);
	temp = DataRead();
	temp &= cClrb5;
	DataWrite(temp);
}

static void LCD_HorizontalWidth_VerticalHeight(uint16_t WX, uint16_t HY) {
	uint8_t temp;

	if (WX < 8) {
		CmdWrite(0x14);
		DataWrite(0x00);

		CmdWrite(0x15);
		DataWrite(WX);
		temp = HY - 1;
		CmdWrite(0x1A);
		DataWrite(temp);

		temp = (HY - 1) >> 8;
		CmdWrite(0x1B);
		DataWrite(temp);
	} else {
		temp = (WX / 8) - 1;
		CmdWrite(0x14);
		DataWrite(temp);

		temp = WX % 8;
		CmdWrite(0x15);
		DataWrite(temp);
		temp = HY - 1;
		CmdWrite(0x1A);
		DataWrite(temp);

		temp = (HY - 1) >> 8;
		CmdWrite(0x1B);
		DataWrite(temp);
	}
}

static void LCD_Horizontal_Non_Display(uint16_t WX) {
	uint8_t temp;

	if (WX < 8) {
		CmdWrite(0x16);
		DataWrite(0x00);

		CmdWrite(0x17);
		DataWrite(WX);
	} else {
		temp = (WX / 8) - 1;
		CmdWrite(0x16);
		DataWrite(temp);

		temp = WX % 8;
		CmdWrite(0x17);
		DataWrite(temp);
	}
}

static void LCD_HSYNC_Start_Position(uint16_t WX) {
	uint8_t temp;

	if (WX < 8) {
		CmdWrite(0x18);
		DataWrite(0x00);
	} else {
		temp = (WX / 8) - 1;
		CmdWrite(0x18);
		DataWrite(temp);
	}
}

static void LCD_HSYNC_Pulse_Width(uint16_t WX) {
	uint8_t temp;

	if (WX < 8) {
		CmdWrite(0x19);
		DataWrite(0x00);
	} else {
		temp = (WX / 8) - 1;
		CmdWrite(0x19);
		DataWrite(temp);
	}
}

static void LCD_Vertical_Non_Display(uint16_t HY) {
	uint8_t temp;

	temp = HY - 1;
	CmdWrite(0x1C);
	DataWrite(temp);

	CmdWrite(0x1D);
	DataWrite(temp >> 8);
}

static void LCD_VSYNC_Start_Position(uint16_t HY) {
	uint8_t temp;

	temp = HY - 1;
	CmdWrite(0x1E);
	DataWrite(temp);
}

static void LCD_VSYNC_Pulse_Width(uint16_t HY) {
	uint8_t temp;

	temp = HY - 1;
	CmdWrite(0x1F);
	DataWrite(temp);
}

//Set_LCD_Panel
static void Set_LCD_Panel(void) {
	//VS070CXN

	LOGI(__FUNCTION__);
	Select_LCD_Sync_Mode(); // Enable XVSYNC, XHSYNC, XDE.

	PCLK_Falling();

	VSCAN_T_to_B();

	PDATA_Set_RGB();

	HSYNC_Low_Active();
	VSYNC_Low_Active();

//    HSYNC_High_Active();
//    VSYNC_High_Active();

	DE_High_Active();

	LCD_HorizontalWidth_VerticalHeight(1024, 600); //INNOLUX 800x480顡�
	LCD_Horizontal_Non_Display(8); //INNOLUX800x600顡�46顡�
	LCD_HSYNC_Start_Position(120); //INNOLUX800x600顡�16~354顡�//210
	LCD_HSYNC_Pulse_Width(6); //INNOLUX800x600顡�1~40顡�	 //10
	LCD_Vertical_Non_Display(8); //INNOLUX800x600顡�23顡�
	LCD_VSYNC_Start_Position(8); //INNOLUX800x600顡�1~147顡�
	LCD_VSYNC_Pulse_Width(6); //INNOLUX800x600顡�1~20顡�
}

//Main_Image_Start_Address
static void Main_Image_Start_Address(uint32_t Addr) {

//	LOGI(__FUNCTION__);
	CmdWrite(0x20);
	DataWrite(Addr);
	CmdWrite(0x21);
	DataWrite(Addr >> 8);
	CmdWrite(0x22);
	DataWrite(Addr >> 16);
	CmdWrite(0x23);
	DataWrite(Addr >> 24);
}

static void Main_Image_Width(uint16_t WX) {
	CmdWrite(0x24);
	DataWrite(WX);
	CmdWrite(0x25);
	DataWrite(WX >> 8);
}

static void Main_Window_Start_XY(uint16_t WX, uint16_t HY) {
	CmdWrite(0x26);
	DataWrite(WX);
	CmdWrite(0x27);
	DataWrite(WX >> 8);
	CmdWrite(0x28);
	DataWrite(HY);
	CmdWrite(0x29);
	DataWrite(HY >> 8);
}

static void Canvas_Image_Start_address(uint32_t Addr) {
	CmdWrite(0x50);
	DataWrite(Addr);
	CmdWrite(0x51);
	DataWrite(Addr >> 8);
	CmdWrite(0x52);
	DataWrite(Addr >> 16);
	CmdWrite(0x53);
	DataWrite(Addr >> 24);
}

static void Canvas_image_width(uint16_t WX) {
	CmdWrite(0x54);
	DataWrite(WX);
	CmdWrite(0x55);
	DataWrite(WX >> 8);
}

static void Active_Window_XY(uint16_t WX, uint16_t HY) {
	CmdWrite(0x56);
	DataWrite(WX);
	CmdWrite(0x57);
	DataWrite(WX >> 8);
	CmdWrite(0x58);
	DataWrite(HY);
	CmdWrite(0x59);
	DataWrite(HY >> 8);
}

static void Active_Window_WH(uint16_t WX, uint16_t HY) {
	CmdWrite(0x5A);
	DataWrite(WX);
	CmdWrite(0x5B);
	DataWrite(WX >> 8);
	CmdWrite(0x5C);
	DataWrite(HY);
	CmdWrite(0x5D);
	DataWrite(HY >> 8);
}

static void Memory_XY_Mode(void) {
	uint8_t temp;

	CmdWrite(0x5E);
	temp = DataRead();
	temp &= cClrb2;
	DataWrite(temp);
}

/*********************** Set_Serial_Flash_IF ************************/
static void Enable_SFlash_SPI(void) {
	uint8_t temp;
	CmdWrite(0x01);
	temp = DataRead();
	temp |= cSetb1;
	DataWrite(temp);
}

static void GTFont_Select_GT30L24T3Y(void) {
	uint8_t temp;
	CmdWrite(0xCE);
	temp = DataRead();
	temp &= cClrb7;
	temp |= cSetb6;
	temp &= cClrb5;
	DataWrite(temp);
}

static void Select_SFI_24bit_Address(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp &= cClrb5;
	DataWrite(temp);
}

static void Select_standard_SPI_Mode0_or_Mode3(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp &= cClrb4;
	DataWrite(temp);
}

static void Select_RA8875_SPI_Mode0_and_Mode3(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp |= cSetb4;
	DataWrite(temp);
}

static void Select_SFI_Dual_Mode_Dummy_8T_3Bh(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp &= 0xF0;
	temp |= 0x02;
	DataWrite(temp);
}

static void Set_CPOL(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp |= cSetb1;
	DataWrite(temp);
}

static void Reset_CPOL(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp &= cClrb1;
	DataWrite(temp);
}

static void Set_CPHA(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp |= cSetb0;
	DataWrite(temp);
}

static void Reset_CPHA(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp &= cClrb0;
	DataWrite(temp);
}

//Set_Serial_Flash_IF
static void Set_Serial_Flash_IF(void) {

	LOGI(__FUNCTION__);
	//W25Q128FVSG
	Enable_SFlash_SPI();
	//GT30L24T3Y
//	GTFont_Select_GT30L24T3Y();
//	Enable_SFlash_SPI();

	Select_SFI_24bit_Address();

//	Select_RA8875_SPI_Mode0_and_Mode3();
	Select_standard_SPI_Mode0_or_Mode3();

	Select_SFI_Dual_Mode_Dummy_8T_3Bh();

	Reset_CPOL();
//	Set_CPOL();

	Reset_CPHA();
//	Set_CPHA();
}

static void Goto_Pixel_XY(uint16_t WX, uint16_t HY) {
	CmdWrite(0x5F);
	DataWrite(WX);
	CmdWrite(0x60);
	DataWrite(WX >> 8);
	CmdWrite(0x61);
	DataWrite(HY);
	CmdWrite(0x62);
	DataWrite(HY >> 8);
}

static void Display_ON(void) {
	uint8_t temp;

	LOGI(__FUNCTION__);
//	dev = open(device, O_RDWR);
	CmdWrite(0x12);
	temp = DataRead();
	temp |= cSetb6;
	DataWrite(temp);
//	close(dev);
}

void Display_OFF(void) {

	uint8_t temp;

	LOGI(__FUNCTION__);
//	dev = open(device, O_RDWR);
	CmdWrite(0x12);
	temp = DataRead();
	temp &= cClrb6;
	DataWrite(temp);
//	close(dev);
}

static void Color_Bar_ON(void) {
	uint8_t temp;

	LOGI(__FUNCTION__);
	CmdWrite(0x12);
	temp = DataRead();
	temp |= cSetb5;
	DataWrite(temp);
}

void Color_Bar_OFF(void) {
	uint8_t temp;

	CmdWrite(0x12);
	temp = DataRead();
	temp &= cClrb5;
	DataWrite(temp);
}
/**************************************** MPU8_Pixels_Memory_Write *********************************************/
static void Check_Mem_WR_FIFO_not_Full(void) {
	uint8_t i;

	for (i = 0; i < WaitTime; i++) {
		if ((StatusRead() & 0x80) == 0) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}
	if (i >= WaitTime) {
		LOGE("Check_Mem_WR_FIFO_not_Full fail");
	}
}

static void Check_Mem_WR_FIFO_Empty(void) {
	uint16_t i;
	for (i = 0; i < WaitTime; i++) {
		if ((StatusRead() & 0x40) == 0x40) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif

	}

	if (i >= WaitTime) {
		LOGE("Check_FIFO_Empty fail");
	}
}

//MPU8_Pixels_Memory_Write
void MPU8_Pixels_Memory_Write(uint16_t x, uint16_t y, uint16_t w, uint16_t h,
		uint8_t *data, uint32_t len) // x of coordinate,y of coordinate ,width,height,8bit data
{
	uint32_t i, PackNum, Remain;
	uint32_t size = len;

//	uint8_t pack[PackSize];
	LOGI(__FUNCTION__);
//	dev = open(device, O_RDWR);

	Graphic_Mode();
	Active_Window_XY(x, y);
	Active_Window_WH(w, h);
	Goto_Pixel_XY(x, y);
	CmdWrite(0x04);

	PackNum = size / PackSize;
	Remain = size % PackSize;

	for (i = 0; i < PackNum; i++) {
		LOGI("finish %d by %d", PackNum, i);
//		memcpy(pack,data+i*PackSize,PackSize);
		WriteOnlyDataArr(data + i * PackSize, PackSize, 0);
		usleep(1000);
	}

	if (Remain != 0) {
//		memcpy(pack,data+PackNum*PackSize,Remain);
		WriteOnlyDataArr(data + i * PackSize, Remain, 1);
	}

	LOGI("finished:p=%d r=%d", PackNum, Remain);
	Check_Mem_WR_FIFO_Empty();

	data = 0;
//	close(dev);
}

/*********************************************** DMA_24bit ***************************************************************/
void Select_SFI_0(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp &= cClrb7;
	DataWrite(temp);
}

void Select_SFI_1(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp |= cSetb7;
	DataWrite(temp);
}

void Select_SFI_DMA_Mode(void) {
	uint8_t temp;
	CmdWrite(0xB7);
	temp = DataRead();
	temp |= cSetb6;
	DataWrite(temp);
}

void SPI_Clock_Period(uint8_t temp) {
	CmdWrite(0xBB);
	DataWrite(temp);
}

void SFI_DMA_Destination_Upper_Left_Corner(unsigned short WX, unsigned short HY) {
	CmdWrite(0xC0);
	DataWrite(WX);
	CmdWrite(0xC1);
	DataWrite(WX >> 8);

	CmdWrite(0xC2);
	DataWrite(HY);
	CmdWrite(0xC3);
	DataWrite(HY >> 8);
}

void SFI_DMA_Transfer_Width_Height(unsigned short WX, unsigned short HY) {
	CmdWrite(0xC6);
	DataWrite(WX);
	CmdWrite(0xC7);
	DataWrite(WX >> 8);
	CmdWrite(0xC8);
	DataWrite(HY);
	CmdWrite(0xC9);
	DataWrite(HY >> 8);
}

void SFI_DMA_Source_Width(unsigned short WX) {
	CmdWrite(0xCA);
	DataWrite(WX);
	CmdWrite(0xCB);
	DataWrite(WX >> 8);
}

void SFI_DMA_Source_Start_Address(unsigned long Addr) {
	CmdWrite(0xBC);
	DataWrite(Addr);
	CmdWrite(0xBD);
	DataWrite(Addr >> 8);
	CmdWrite(0xBE);
	DataWrite(Addr >> 16);
	CmdWrite(0xBF);
	DataWrite(Addr >> 24);
}

void Start_SFI_DMA(void) {
	uint8_t temp;
	CmdWrite(0xB6);
	temp = DataRead();
	temp |= cSetb0;
	DataWrite(temp);
}

void Check_Busy_SFI_DMA(void) {
	uint8_t i = 0;
	CmdWrite(0xB6);
	for (i = 0; i < WaitTime; i++) {
		if ((DataRead() & 0x01) == 0x01) {
			break;
		}
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}
	if (i >= WaitTime) {
		LOGE("Check_Busy_SFI_DMA fail");
	}
}
//(1,0,0,0,480,1920,480,0);
void DMA_24bit(uint8_t SCS, uint8_t Clk, uint16_t X1, uint16_t Y1, uint16_t X_W,
		uint16_t Y_H, uint16_t P_W, uint32_t Addr) {
	LOGI(__FUNCTION__);
	if (SCS == 0) {
		Select_SFI_0(); //Select Serial Flash 0
	}
	if (SCS == 1) {
		Select_SFI_1(); //Select Serial Flash 1
	}

	Select_SFI_DMA_Mode(); //Set Serial Flash DMA Mode
	SPI_Clock_Period(Clk);

	Goto_Pixel_XY(X1, Y1); //set Memory coordinate in Graphic Mode
	SFI_DMA_Destination_Upper_Left_Corner(X1, Y1); //DMA Destination position(x,y)
	SFI_DMA_Transfer_Width_Height(X_W, Y_H);
	SFI_DMA_Source_Width(P_W); //Set DMA Source Picture Width
	SFI_DMA_Source_Start_Address(Addr);

	Start_SFI_DMA(); //Start DMA
	Check_Busy_SFI_DMA();
}

/********************************************** BTE_Solid_Fill ************************************************************************/
void Foreground_color_16M(unsigned long temp) {
	CmdWrite(0xD2);
	DataWrite(temp >> 16);

	CmdWrite(0xD3);
	DataWrite(temp >> 8);

	CmdWrite(0xD4);
	DataWrite(temp);
}

void BTE_Destination_Memory_Start_Address(unsigned long Addr) {
	RegisterWrite(0xA7, Addr);
	RegisterWrite(0xA8, Addr >> 8);
	RegisterWrite(0xA9, Addr >> 16);
	RegisterWrite(0xAA, Addr >> 24);
}

void BTE_Destination_Image_Width(unsigned short WX) {
	RegisterWrite(0xAB, WX);
	RegisterWrite(0xAC, WX >> 8);
}

void BTE_Destination_Window_Start_XY(unsigned short WX, unsigned short HY) {
	RegisterWrite(0xAD, WX);
	RegisterWrite(0xAE, WX >> 8);

	RegisterWrite(0xAF, HY);
	RegisterWrite(0xB0, HY >> 8);
}

void BTE_Window_Size(unsigned short WX, unsigned short HY) {
	RegisterWrite(0xB1, WX);
	RegisterWrite(0xB2, WX >> 8);

	RegisterWrite(0xB3, HY);
	RegisterWrite(0xB4, HY >> 8);
}

void BTE_Enable(void) {
	uint8_t temp;
	CmdWrite(0x90);
	temp = DataRead();
	temp |= cSetb4;
	DataWrite(temp);
}

void BTE_Operation_Code(uint8_t setx) {
	uint8_t temp;
	CmdWrite(0x91);
	temp = DataRead();
	temp &= 0xf0;
	temp |= setx;
	DataWrite(temp);

}

void BTE_Solid_Fill(unsigned long Des_Addr, uint16_t Des_W, uint16_t XDes,
		uint16_t YDes, unsigned long Foreground_color, uint16_t X_W,
		uint16_t Y_H) {
	Foreground_color_16M(Foreground_color);

	BTE_Destination_Memory_Start_Address(Des_Addr);
	BTE_Destination_Image_Width(Des_W);
	BTE_Destination_Window_Start_XY(XDes, YDes);
	BTE_Window_Size(X_W, Y_H);
	BTE_Operation_Code(0x0c);
	BTE_Enable();
}
/************************************************ FLASH ********************************************************/

uint8_t Tx_FIFO_Empty_Flag(void) {
	CmdWrite(0xBA);
	if ((DataRead() & 0x80) == 0x80)
		return 1;
	else
		return 0;
}

uint8_t Rx_FIFO_Empty_Flag(void) {
	CmdWrite(0xBA);
	if ((DataRead() & 0x20) == 0x20)
		return 1;
	else
		return 0;
}

uint8_t SPI_Master_FIFO_Data_Get(void) {
	uint8_t temp;
	uint8_t i;

	for (i = 0; i < WaitTime; i++) {
		if (Rx_FIFO_Empty_Flag() != 1)
			break;
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}
	CmdWrite(0xB8);
	temp = DataRead();
	return temp;
}

uint8_t SPI_Master_FIFO_Data_Put(uint8_t Data) {
	uint8_t temp;
	uint8_t i;

	CmdWrite(0xB8);
	DataWrite(Data);
	for (i = 0; i < WaitTime; i++) {
		if (Tx_FIFO_Empty_Flag() != 0)
			break;
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}

	temp = SPI_Master_FIFO_Data_Get();

	return temp;
}

void nSS_Active(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp |= cSetb4;
	DataWrite(temp);
}

void nSS_Inactive(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp &= cClrb4;
	DataWrite(temp);
}

uint8_t RDSR(void) {
	uint8_t temp;

	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x05);
	usleep(1);
	temp = SPI_Master_FIFO_Data_Put(0xff); //dummy cycle and read back value

	nSS_Inactive();
	return (temp);
}

void WREN(void) {
	uint8_t i;

	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x06); //Serial Flash WREN
	nSS_Inactive();

	for (i = 0; i < WaitTime; i++) {
		if (RDSR() & 0x02)
			break;
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	}
}

void WRDI(void) {
	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x04); //Serial Flash WRDI
	//while( EMTI_Flag()!=1);
	// Clear_EMTI_Flag();
	usleep(1);
	nSS_Inactive();
}

/*void SPI_Clock_Period(uint8_t temp)
 {
 CmdWrite(0xBB);
 DataWrite(temp);
 } */

void Select_nSS_drive_on_xnsfcs0(void)
{
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp &= cClrb5;
	DataWrite(temp);

}

void Select_nSS_drive_on_xnsfcs1(void) {
	uint8_t temp;
	CmdWrite(0xB9);
	temp = DataRead();
	temp |= cSetb5;
	DataWrite(temp);

}

void CE(void) {

	WREN();

	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x60); //Serial Flash Chip Erase
	nSS_Inactive();

	WRDI();

}

#define	SPI_bufLen	17
#define	SPI_dataLen	(SPI_bufLen - 1)
static void SPI_Master_FIFO_DataArr(const uint8_t *data, uint32_t len,
		uint8_t cs) {
	int ret;
	uint32_t i;
	uint16_t bufRemain = len % SPI_dataLen;
	uint32_t bufNum = (bufRemain == 0) ? len / SPI_dataLen : len / SPI_dataLen + 1;
	//	uint8_t		txbuf[bufNum][bufLen];
	uint8_t *txbuf[bufNum];

	//	struct spi_ioc_transfer xfer[bufNum];

	//	memset(xfer, 0, sizeof xfer);
	//	memset(txbuf, 0,sizeof txbuf);
	LOGI("SPI_Master_FIFO_DataArr");
	LOGI("bufNum = %d,bufRemain = %d", bufNum, bufRemain);



//	bufNum = 16;
//	bufRemain = 0;
	for (i = 0; i < bufNum; i++) {
		txbuf[i] = malloc(SPI_bufLen * (sizeof(uint8_t)));
		txbuf[i][0] = 0xB8;
		if ((i == bufNum - 1) && (bufRemain != 0)) {
			//			LOGI("doing bufRemain %d",i);
			memcpy(txbuf[i] + 1, data + i * SPI_dataLen, bufRemain);

			write(dev, txbuf[i], bufRemain + 1);

		} else {
			//			LOGI("doing bufNum %d",i);
			memcpy(txbuf[i] + 1, data + i * SPI_dataLen, SPI_dataLen);

			write(dev, txbuf[i], SPI_bufLen);

		}
	}

	//	ret = ioctl(dev, SPI_IOC_MESSAGE(bufNum), xfer);
	//	LOGI("sizeof(xfer) = %d,sizeof(struct spi_ioc_transfer) = %d",sizeof(xfer),sizeof(struct spi_ioc_transfer));
	//	ret = write(dev, xfer, bufNum*sizeof(struct spi_ioc_transfer));
	LOGI("SPI_Master_FIFO_DataArr Finish");
	if (ret < 1)
		LOGE("%s ;can't send spi message", __FUNCTION__);

	for (i = 0; i < bufNum; i++) {
		free(txbuf[i]);
	}
}


//test
//write status reg
unsigned char WRSR(unsigned char SR_data)
{
  unsigned char temp;
 	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x01);
	usleep(1);
	temp=SPI_Master_FIFO_Data_Put(SR_data);   //dummy cycle and read back value
	nSS_Inactive();
	return(temp);
}


//read data from flash
unsigned char Read_FlashData(void)
{
  unsigned char temp;
 	nSS_Active();
	SPI_Master_FIFO_Data_Put(0x03);
	usleep(1);
	temp=SPI_Master_FIFO_Data_Put(0xff);   //dummy cycle and read back value
	nSS_Inactive();
	return(temp);
}

//#define CE_ENABLE 1
#define FIFOPack   15
static void SPI_Master_FIFO_Write(const uint8_t *data, uint32_t len,
		uint32_t flash_addr) {
	uint32_t i, j,k, PackNum=0, Remain=0;
	uint32_t size = 300;

	uint8_t pack[FIFOPack];

	uint8_t testbuf[size];
	uint32_t addr = 0;
	//

	Select_nSS_drive_on_xnsfcs1();
	SPI_Clock_Period(1);

//#ifdef CE_ENABLE
	CE();
	LOGI("%s:Chip Erase", __FUNCTION__);

	 for(i=0;i<30;i++){
	 if(!(RDSR()& 0x01))
		 break;
#ifdef TIMEOUT_WARNNING
		else
		{
			usleep(1000 * 1000);
			LOGE("%s->Time out:%d ", __FUNCTION__,i);
		}
#endif
	 }
	 usleep(1000 * 1000);
//#else
/*
	Set_Serial_Flash_IF();
	SPI_Clock_Period(1);
	Select_nSS_drive_on_xnsfcs1();
*/

	LOGI("%s:Write", __FUNCTION__);



	for(k=0;k<10800;k++)
	{
		WREN();
		nSS_Active();
		SPI_Master_FIFO_Data_Put(0x02);
		SPI_Master_FIFO_Data_Put((uint8_t)((addr & 0xff0000) >> 16));
		SPI_Master_FIFO_Data_Put((uint8_t)((addr & 0xff00) >> 8));
		SPI_Master_FIFO_Data_Put((uint8_t)(addr & 0xff));

		SPI_Master_FIFO_DataArr(data+k*128, 128, 1);

		/*
			for (j = 0; j < 256; j++)
			{
				i = SPI_Master_FIFO_Data_Put(data[j]);
				//LOGD("j=%d SPI_Master_FIFO_Data_Get = 0x%02x",j, i);
			}
		*/

		addr+=128;

		nSS_Inactive();
		 for(i=0;i<WaitTime;i++){
		 if(!(RDSR()& 0x01))
			 break;
	#ifdef TIMEOUT_WARNNING
			else
			{
				usleep(1);
				LOGE("%s->Time out:%d ", __FUNCTION__,i);
			}
	#endif
		 }


	/*		for(j=0;j<10;j++)
			{
				WRSR(j);
				usleep(10);
				LOGD("j = 0x%02x , Read_FlashData = 0x%02x",j, Read_FlashData());
			}*/

		 WRDI();
	}
//#endif







}



/****************************************************** JNI ********************************************************************/
/*
 * Class:     com_jackie_ts0210_drive_RA8876L
 * Method:    init
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_itc_ts8209a_drive_Ra8876l_init
  (JNIEnv *env, jclass obj, jint spd)
{
	if(dev >= 0){
		close(dev);
		LOGI("after close dev = %d", dev);
		dev = -1;
	}
	dev = open(device, O_RDWR | O_NONBLOCK);
//	LOGI("after open dev = %d",dev);
	SPI_Config(spd);
	
	System_Check();
	Check_IC();
	PLL_init();
	SDRAM_init();
#ifdef ColorDepth_24bit
	LOGI("ColorDepth_24bit");
	TFT_24bit();
#endif

#ifdef ColorDepth_16bit
	LOGI("ColorDepth_16bit");
	TFT_16bit();
#endif
	
	Host_Bus_8bit();
	
#ifdef ColorDepth_24bit	
	Data_Format_8b_24bpp();
#endif	

#ifdef ColorDepth_16bit
	Data_Format_8b_16bpp();
#endif
	MemWrite_Left_Right_Top_Down();
//	MemWrite_Right_Left_Top_Down();
//	MemWrite_Down_Top_Left_Right();
	
	Graphic_Mode();
	
	Memory_Select_SDRAM();
#ifdef ColorDepth_24bit		
	Select_Main_Window_24bpp();		//[10h]Set main window color depth
	Memory_24bpp_Mode();
	Select_PIP1_Window_24bpp();		//[11h] PIP 1 Window Color Depth
	Select_PIP2_Window_24bpp();	
	BTE_S0_Color_24bpp();			//[92h] Source_0 Color Depth
	BTE_S1_Color_24bpp();			//[92h] Source_1 Color Depth
	BTE_Destination_Color_24bpp();
#endif		

#ifdef ColorDepth_16bit
	Select_Main_Window_16bpp();		//[10h]Set main window color depth
	Memory_16bpp_Mode();
	Select_PIP1_Window_16bpp();		//[11h] PIP 1 Window Color Depth
	Select_PIP2_Window_16bpp();	
	BTE_S0_Color_16bpp();			//[92h] Source_0 Color Depth
	BTE_S1_Color_16bpp();			//[92h] Source_1 Color Depth
	BTE_Destination_Color_16bpp();
#endif
	Set_LCD_Panel();
	
	Main_Image_Start_Address(0);
	
	Main_Image_Width(Panel_width);
	
	Main_Window_Start_XY(0,0);
	
	Canvas_Image_Start_address(0);
	
	Canvas_image_width(canvus_width);
	
	Active_Window_XY(0,0);
	
	Active_Window_WH(Panel_width,Panel_length);
	
	Memory_XY_Mode();
	
	Set_Serial_Flash_IF();
	
	Goto_Pixel_XY(0,0);
	
	
//	DMA_24bit(1,0,0,0,480,1920,480,0);	
	Display_ON();

//	close(dev);
}

/*
 * Class:     com_jackie_ts0210_drive_RA8876L
 * Method:    sendData
 * Signature: (SSSS[BI)V
 */


JNIEXPORT void JNICALL Java_com_itc_ts8209a_drive_Ra8876l_sendData
(JNIEnv *env, jclass obj, jshort x, jshort y,jshort w, jshort h, jbyteArray data, jint len){
	uint32_t i;
	//jbyte* bBuffer = (*env)->GetByteArrayElements(env,data,0);
	uint8_t* dataBuf = (uint8_t*)((*env)->GetByteArrayElements(env,data,0));

	MPU8_Pixels_Memory_Write(x,y,w,h,dataBuf,len);

	(*env)-> ReleaseByteArrayElements(env, data, (jbyte *)dataBuf, 0);

}

/*
 * Class:     com_jackie_ts0210_drive_RA8876L
 * Method:    handShake
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_itc_ts8209a_drive_Ra8876l_handShake
  (JNIEnv *env, jclass obj){
	  LOGI("TS-8209A Handshake to drive");
}

/*
 * Class:     com_jackie_ts0210_drive_RA8876L
 * Method:    displayOn
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_itc_ts8209a_drive_Ra8876l_displayOn
(JNIEnv *env, jclass obj) {
Display_ON();
}

/*
 * Class:     com_jackie_ts0210_drive_RA8876L
 * Method:    displayOff
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_itc_ts8209a_drive_Ra8876l_displayOff
(JNIEnv *env, jclass obj) {
Display_OFF();
}

#ifdef __cplusplus
}
#endif
