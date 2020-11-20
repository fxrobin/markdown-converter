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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.pdf.converter.PdfConverterExtension;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

@Path("/api/v1")
public class MarkdownConverter {

	private static DataHolder OPTIONS = new MutableDataSet()
			.set(Parser.REFERENCES_KEEP, KeepType.LAST)
			.set(HtmlRenderer.INDENT_SIZE, 2)
			.set(HtmlRenderer.PERCENT_ENCODE_URLS, true)

			// for full GFM table compatibility add the following table extension options:
			.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
			.set(EmojiExtension.ROOT_IMAGE_PATH, "http://localhost:8080/img/")
			.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), EmojiExtension.create()))
			.toImmutable();

	@Path("/conversion")
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.TEXT_PLAIN)
	public String convertToHtml(@NotNull String fullMarkdownText) {
		return convert(fullMarkdownText);
	}

	@Path("/conversion")
	@POST
	@Produces("application/pdf")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response convertToPdf(@NotNull String fullMarkdownText) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String html = convert(fullMarkdownText);
    	PdfConverterExtension.exportToPdf(out, html , "", OPTIONS);
		String fileName = String.format("document-%s.pdf", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
		String contentDisposition = String.format("Content-Disposition", "attachment; filename=%s", fileName);				
		ResponseBuilder response = Response.ok(out.toByteArray());
		return response.header("Content-Disposition", contentDisposition).build();
	}

	
	// factorisation, car utilisée par les deux méthodes proposées par l'API.
	private String convert(String fullMarkdownText) {
		Parser parser = Parser.builder(OPTIONS).build();
		Node document = parser.parse(fullMarkdownText);
		HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
		return renderer.render(document);
	}

}