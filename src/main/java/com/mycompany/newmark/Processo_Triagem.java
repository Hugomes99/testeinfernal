/**
 * @author Felipe Marques, Gabriel Ramos, Rafael Henrique e Adriano Vilhena 
 * 
 */
package com.mycompany.newmark;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author admin-felipe
 */
public class Processo_Triagem {

	public Processo_Triagem(WebDriver driver, String banco) throws ParseException {
		super();
	}

	//Inicia a Triagem
	public boolean iniciarTriagem(WebDriver driver, WebDriverWait wait, String bancos, boolean triagemIniciada)
			throws SQLException, InterruptedException, UnsupportedFlavorException, IOException {
		Actions actions = new Actions(driver);
		Chaves_Resultado resultado = new Chaves_Resultado();
		resultado.setEtiqueta("NÃO FOI POSSÍVEL LOCALIZAR FRASE CHAVE ATUALIZADA");
		resultado.setDriver(driver);
		Chaves_Configuracao config = new Chaves_Configuracao();
		Banco banco = new Banco();
		config = banco.pegarConfiguracao(config);
		Processo_Grid processo_grid = new Processo_Grid(resultado.getDriver());
		Processo_Movimentacao movimentacao = new Processo_Movimentacao();
		Processo_Documento documento = new Processo_Documento();
		Processo_Pericial pericial = new Processo_Pericial();
		Processo_PeticaoInicial peticao = new Processo_PeticaoInicial();
		Processo_Etiquetar etiqueta = new Processo_Etiquetar();
		boolean grid;
		try {
			if (triagemIniciada == false) {
				// Adiciona a coluna "Órgão Julgador" no Grid inicial
				//wait: aguarda por 15s até que a condição seja verdadeira
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"gridcolumn-1083-titleEl\"]")));
				WebElement target = driver.findElement(By.xpath("//*[@id=\"gridcolumn-1083-titleEl\"]"));
				actions.moveToElement(target).perform();
				wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"gridcolumn-1083-triggerEl\"]")));
				driver.findElement(By.xpath("//*[@id=\"gridcolumn-1083-triggerEl\"]")).click();
				//wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"menuitem-2688-itemEl\"]")));
				//driver.findElement(By.xpath("//*[@id=\"menuitem-2688-itemEl\"]")).click();
				Thread.sleep(500);
				/*
				actions.sendKeys(Keys.ARROW_DOWN).build().perform();
				actions.sendKeys(Keys.ARROW_DOWN).build().perform();
				actions.sendKeys(Keys.ARROW_DOWN).build().perform();
				actions.sendKeys(Keys.ARROW_RIGHT).build().perform();
				*/
				driver.findElement(By.linkText("Colunas")).click();
				driver.findElement(By.linkText("Órgão Julgador")).click();
				actions.sendKeys(Keys.ESCAPE).perform();
				Thread.sleep(500);
			}
			wait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridview-1109-body")));
			wait.until(ExpectedConditions.elementToBeClickable(By.id("gridview-1109-body")));

			do {
				resultado = processo_grid.buscar_processo(resultado.getDriver(), wait);
				grid = resultado.isGrid();
				if (grid == false) {
					return true;
				} else {
					//Setar todas as configurações especificas nesses ifs
					if (config.isLaudoPericial() == true) {
						resultado = pericial.pericial(resultado.getDriver(), wait, bancos);
					} else if (config.isPeticaoInicial() == true) {
						resultado = peticao.peticaoInicial(resultado.getDriver(), wait, config, bancos,
								resultado.getOrgaoJulgador());
					} else {
						switch (config.getTipoTriagem()) {
						case "COM":
							resultado = movimentacao.movimentacao(resultado.getDriver(), wait, config, bancos);
							if (resultado.getEtiqueta().contains("NÃO FOI POSSÍVEL LOCALIZAR FRASE CHAVE ATUALIZADA")) {
								resultado = documento.documento(resultado.getDriver(), wait, config, bancos);
							}
							break;
						case "MOV":
							resultado = movimentacao.movimentacao(resultado.getDriver(), wait, config, bancos);
							break;
						case "DOC":
							resultado = documento.documento(resultado.getDriver(), wait, config, bancos);
							;
							break;
						}
					}
					//Fecha a janela do processo e volta para a janela do grid
					List<String> janela = new ArrayList(resultado.getDriver().getWindowHandles());
					resultado.getDriver().switchTo().window(janela.get(1)).close();
					resultado.getDriver().switchTo().window(janela.get(0));

					etiqueta.etiquetar(resultado.getDriver(), wait, resultado);
					//Contadores
					if (resultado.getLocal().contains("Mov")) {
						banco.contarMov();
					} else if (resultado.getLocal().contains("Doc")) {
						banco.contarDoc();
					} else {
						banco.contarNao();
					}
				}
			} while (grid != false);
		} catch (Exception e) {
			final Exception ex = e;
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Alert erro = new Alert(Alert.AlertType.CONFIRMATION);
					erro.setTitle("Alerta");
					erro.setHeaderText("Ocorreu um erro duranto o processo de triagem!");
					erro.setContentText("Deseja submeter este erro para os desenvolvedores?\n");
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					String exceptionText = sw.toString();

					Label label = new Label("The exception stacktrace was:");

					TextArea textArea = new TextArea(exceptionText);
					textArea.setEditable(false);
					textArea.setWrapText(true);
					textArea.setMaxWidth(Double.MAX_VALUE);
					textArea.setMaxHeight(Double.MAX_VALUE);
					GridPane.setVgrow(textArea, Priority.ALWAYS);
					GridPane.setHgrow(textArea, Priority.ALWAYS);

					GridPane expContent = new GridPane();
					expContent.setMaxWidth(Double.MAX_VALUE);
					expContent.add(label, 0, 0);
					expContent.add(textArea, 0, 1);
					// Set expandable Exception into the dialog pane.
					erro.getDialogPane().setExpandableContent(expContent);
					erro.showAndWait();
					if (erro.getResult() == ButtonType.OK) {
						final UtilEmail email = new UtilEmail(exceptionText);
						Runnable task = new Runnable() {
							@Override
							public void run() {
								email.enviarEmail();
							}
							
						};
						Thread mail = new Thread(task);
						mail.setDaemon(true);
						mail.start();					
					}
				}
			});
			return true;
		}
		return true;
	}
}