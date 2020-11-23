package fr.fxjavadevblog.mc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.spi.HttpResponse;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;


@Path("/api/v1")
public class MarkdownConverter {
	
	//@Context private HttpResponse response;

	private static DataHolder OPTIONS = new MutableDataSet()
			.set(Parser.REFERENCES_KEEP, KeepType.LAST)
			.set(HtmlRenderer.INDENT_SIZE, 2)
			.set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
			.set(HtmlRenderer.GENERATE_HEADER_ID, true)

			// for full GFM table compatibility add the following table extension options:
			.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
			.set(EmojiExtension.ROOT_IMAGE_PATH, "http://localhost:8080/img/")
			.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), 
						                EmojiExtension.create(),						                
						                AbbreviationExtension.create(),
						                TaskListExtension.create(),
						                GitLabExtension.create(),
						                TocExtension.create(),
						                YamlFrontMatterExtension.create()
					                ))
			.toImmutable();

	
	@Path("conversion")
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.TEXT_PLAIN)
	public String convertToHtml(@NotNull String fullMarkdownText) {
		return convert(fullMarkdownText);
	}

	
	@Path("conversion")
	@POST
	@Produces("application/pdf")
	@Consumes(MediaType.TEXT_PLAIN)
	public byte[] convertToPdf(@NotNull String fullMarkdownText, @Context HttpResponse response) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
    	PdfConverterExtension.exportToPdf(out, convert(fullMarkdownText) , "", OPTIONS); 
		generateContentDispositionFileName(response);	
		return out.toByteArray();
	}


	/**
	 * génère le header "Content-Disposition" pour proposer un nom de fichier au téléchargement.
	 * 
	 * @param response
	 */
	private static void generateContentDispositionFileName(HttpResponse response) {
		String fileName = String.format("document-%s.pdf", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
		String contentDisposition = String.format("attachment; filename=%s", fileName);				
		response.getOutputHeaders().add("Content-Disposition" , contentDisposition);
	}
	
	// factorisation, car utilisée par les deux méthodes proposées par l'API.
	private static String convert(String fullMarkdownText) {
		Parser parser = Parser.builder(OPTIONS).build();
		Node document = parser.parse(fullMarkdownText);
		HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
		return renderer.render(document);
	}

}