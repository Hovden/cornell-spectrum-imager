
/* ImageJ Macro "Hanning Window"

Authors: Roberto Sotto-Maior Fortes de Oliveira,Vera Maria Peters,Robert Willer Farinazzo Vitral
Address: Universidade Federal de Juiz de Fora - Brazil
Email: R.Sotto-Maior (roberto at aparelho dot com) 

This Macro generates a Hanning Window Function Image to be used in Discrete Fourier Transform computing applications.
The windowing function image size should be informed in the dialog Window.
This code produces a 32 bit image Hanning Window with equal height and width dimensions.
	
References:

Burger,W.,Burge,M.,2008.Digital image processing:an algorithmic introduction using Java,1st ed.Springer,New York.p.352-358.
R.Sotto-Maior.Errata.Table 14.3:error in equation for Hanning windowing function.http://www.imagingbook.com/fileadmin/en/errata1/ch14-355.pdf.

Acknowledgement: The support from FAPEMIG (Fundacao de Amparo a Pesquisa de Minas Gerais) is gratefully acknowledged. 

History: 2009/7/25:Firt version 

*/

macro "Hanning Window" {

Dialog.create("Hanning Window");
Dialog.addNumber("Window size:", 512);
Dialog.show();
size = Dialog.getNumber();

setBatchMode(true);
newImage("Hanning Window", "32-bit Black", size, size, 1);
M=size;
N=size;

for (v=0; v<N; v++) {
	showProgress(v, N);
	for (u=0; u<M; u++) {
		ru=2*u/M-1;
		rv = 2*v/N-1;
		ruv=pow(ru,2)+pow(rv,2);
		ruv = sqrt(ruv);
		wuv =  0.5*(cos(PI*ruv)+1);
		if (ruv >= 0 && ruv < 1) 
			setPixel(u, v, 1*wuv);
		else
			setPixel(u, v, 0);
	}
}
setBatchMode(false);
}
