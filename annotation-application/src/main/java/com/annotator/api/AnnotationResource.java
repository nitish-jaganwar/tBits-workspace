package com.annotator.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.annotator.config.DBConfig; // Import DB Helper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/annotations")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationResource {

	// 1. SAVE API
	@POST
	@Path("/save/{docId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveAnnotations(@PathParam("docId") String docId, String jsonPayload) {
		try (Connection conn = DBConfig.getConnection()) {
			String sql = "INSERT INTO document_annotations (doc_id, json_data) VALUES (?, ?) "
					+ "ON DUPLICATE KEY UPDATE json_data = ?";

			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, docId);
			pstmt.setString(2, jsonPayload);
			pstmt.setString(3, jsonPayload);

			pstmt.executeUpdate();
			return Response.ok("{\"status\":\"success\", \"message\":\"Saved to DB\"}").build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	// 2. LOAD API
	@GET
	@Path("/load/{docId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response loadAnnotations(@PathParam("docId") String docId) {
		try (Connection conn = DBConfig.getConnection()) {
			String sql = "SELECT json_data FROM document_annotations WHERE doc_id = ?";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, docId);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return Response.ok(rs.getString("json_data")).build();
			} else {
				return Response.ok("{\"annotations\":[], \"canvasData\":null}").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	// 3. GET ORIGINAL FILE
	@GET
	@Path("/file/{docId}")
	@Produces({ "application/pdf", "image/jpeg", "image/png" })
	public Response getOriginalDocument(@PathParam("docId") String docId) {
		try {

			java.util.Properties props = new java.util.Properties();
			java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");

			if (input == null) {
				return Response.serverError().entity("Error: config.properties file not found!").build();
			}
			props.load(input);
			String folderPath = props.getProperty("file.storage.path");

			File file = new File(folderPath + docId + ".pdf");

			if (!file.exists()) {
				return Response.status(Response.Status.NOT_FOUND).entity("Error: Document not found for ID -> " + docId)
						.build();
			}

			return Response.ok(file).header("Content-Disposition", "inline; filename=\"" + file.getName() + "\"")
					.build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity("Server error: " + e.getMessage()).build();
		}
	}

	// 4. EXPORT WITH PDFBOX
	@GET
	@Path("/export/pdfbox/{docId}")
	@Produces("application/pdf")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public Response exportWithPdfBox(@PathParam("docId") String docId) {
		String safeDocId = docId.toUpperCase();
		File file = new File("C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\annot_files\\" + safeDocId + ".pdf");

		if (!file.exists()) {
			return Response.status(Response.Status.NOT_FOUND).entity("Error: File not found").type("text/plain")
					.build();
		}

		String jsonPayload = null;
		String query = "SELECT json_data FROM document_annotations WHERE doc_id = ? LIMIT 1";

		try (Connection conn = DBConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, safeDocId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				jsonPayload = rs.getString("json_data");
			}
		} catch (Exception e) {
			return Response.serverError().entity("Database error: " + e.getMessage()).build();
		}

		if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
			return Response.ok(file)
					.header("Content-Disposition", "inline; filename=\"" + safeDocId + "_Original.pdf\"").build();
		}

		try (PDDocument document = PDDocument.load(file)) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(jsonPayload);
			JsonNode annotationsArray = rootNode.path("annotations");
			JsonNode canvasObjects = rootNode.path("canvasData").path("objects");

			final float SCALE_FACTOR = 1.5f; // because in frontend we are using 1.5 scale for better visibility, so we
												// need to reverse that for PDF coordinates

			for (JsonNode anno : annotationsArray) {
				if (anno.path("isDraft").asBoolean(true))
					continue;

				String id = anno.path("id").asText();
				String type = anno.path("type").asText("Rectangle").toLowerCase();
				JsonNode shapeNode = null;

				if (canvasObjects.isArray()) {
					for (JsonNode obj : canvasObjects) {
						if (id.equals(obj.path("id").asText())) {
							shapeNode = obj;
							break;
						}
					}
				}

				if (shapeNode != null) {
					float scaleX = (float) shapeNode.path("scaleX").asDouble(1.0);
					float scaleY = (float) shapeNode.path("scaleY").asDouble(1.0);
					float pdfX = (float) (shapeNode.path("left").asDouble() / SCALE_FACTOR);
					float pdfY_fromTop = (float) (shapeNode.path("top").asDouble() / SCALE_FACTOR);
					float pdfWidth = (float) ((shapeNode.path("width").asDouble() * scaleX) / SCALE_FACTOR);
					float pdfHeight = (float) ((shapeNode.path("height").asDouble() * scaleY) / SCALE_FACTOR);

					int pageIndex = anno.path("page").asInt(1) - 1;
					if (pageIndex < 0 || pageIndex >= document.getNumberOfPages())
						pageIndex = 0;
					PDPage page = document.getPage(pageIndex);

					float pageHeight = page.getMediaBox().getHeight();
					float finalPdfY = pageHeight - pdfY_fromTop - pdfHeight;

					String hexColor = anno.path("color").asText("#3b6ef8").replace("#", "");
					org.apache.pdfbox.pdmodel.graphics.color.PDColor pdColor = null;
					if (hexColor.length() == 6) {
						float r = Integer.parseInt(hexColor.substring(0, 2), 16) / 255f;
						float g = Integer.parseInt(hexColor.substring(2, 4), 16) / 255f;
						float b = Integer.parseInt(hexColor.substring(4, 6), 16) / 255f;
						pdColor = new org.apache.pdfbox.pdmodel.graphics.color.PDColor(new float[] { r, g, b },
								org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB.INSTANCE);
					}

					String userName = anno.path("createdBy").path("name").asText("Reviewer");
					String commentText = anno.path("text").asText("No comments");

					if (type.contains("arrow") || type.contains("line")) {
						org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine lineAnnot = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine();
						org.apache.pdfbox.pdmodel.common.PDRectangle pdRect = new org.apache.pdfbox.pdmodel.common.PDRectangle(
								pdfX, finalPdfY, pdfWidth, pdfHeight);
						lineAnnot.setRectangle(pdRect);

						lineAnnot.setLine(new float[] { pdfX, finalPdfY + pdfHeight, pdfX + pdfWidth, finalPdfY });
						if (type.contains("arrow"))
							lineAnnot.setEndPointEndingStyle(
									org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine.LE_CLOSED_ARROW);

						lineAnnot.setContents(commentText);
						lineAnnot.setTitlePopup(userName);
						lineAnnot.setAnnotationName(userName);
						if (pdColor != null)
							lineAnnot.setColor(pdColor);
						page.getAnnotations().add(lineAnnot);
					} else {
						String subType = type.contains("ellipse") || type.contains("circle")
								? org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle.SUB_TYPE_CIRCLE
								: org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle.SUB_TYPE_SQUARE;

						org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle annotation = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle(
								subType);
						org.apache.pdfbox.pdmodel.common.PDRectangle pdRect = new org.apache.pdfbox.pdmodel.common.PDRectangle(
								pdfX, finalPdfY, pdfWidth, pdfHeight);
						annotation.setRectangle(pdRect);
						annotation.setContents(commentText);
						annotation.setAnnotationName(userName);
						annotation.setTitlePopup(userName);
						if (pdColor != null)
							annotation.setColor(pdColor);
						page.getAnnotations().add(annotation);
					}
				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			document.save(baos);

			return Response.ok(baos.toByteArray()).type("application/pdf")
					.header("Content-Disposition", "attachment; filename=\"" + safeDocId + "_Annotated.pdf\"").build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity("PDF Generation Failed: " + e.getMessage()).build();
		}
	}
}