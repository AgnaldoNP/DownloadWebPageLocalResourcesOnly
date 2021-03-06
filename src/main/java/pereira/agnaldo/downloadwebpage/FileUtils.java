package pereira.agnaldo.downloadwebpage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import pereira.agnaldo.downloadwebpage.MainUI.DonwloadTask;

public class FileUtils {

	public static String getDirBase() {

		final String dir = "";
		mkdirs(dir);
		return dir;
	}

	private static void mkdirs(final String dir) {
		final File file = new File(dir);
		if (!file.exists()) {
			file.mkdirs();
			try {
				final String command = "chmod 777 " + file.getAbsolutePath();
				Runtime.getRuntime().exec(command);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static String getNameFileFromURL(final String url) {
		try {
			String name = null;
			if (url != null && !"".equals(url)) {
				String decodedUrl = url;
				if (decodedUrl.contains("?")) {
					name = decodedUrl.substring(decodedUrl.lastIndexOf("/") + 1, decodedUrl.indexOf("?"));
				} else {
					name = decodedUrl.substring(decodedUrl.lastIndexOf("/") + 1);
				}
			}
			return name;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void storeStringOnFile(final String content, final String filePath) throws Exception {
		FileWriter fileWriter = new FileWriter(filePath);
		fileWriter.write(content);
		fileWriter.flush();
		fileWriter.close();
	}

	public static void copyURLToFile(final String urlPage) throws Exception {
		String path = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
		File mediaFile = new File(path, "TesteDownloadWebPage");
		mkdirs(mediaFile.getAbsolutePath());

		mediaFile = new File(mediaFile.getAbsolutePath(), "teste.html");
		try {
			final Connection.Response response = Jsoup.connect(urlPage).execute();
			final Document doc = response.parse();
			final String baseUri = doc.baseUri();
			final ArrayList<String> sourcesToDownload = new ArrayList<>();
			final String[] attributeNames = new String[] { "src", "href" };

			final Elements elements = new Elements();
			for (final String attributeName : attributeNames) {
				elements.addAll(doc.getElementsByAttribute(attributeName));
			}

			String outerHtml = doc.outerHtml();

			for (final Element element : elements) {
				String attr = null;
				String attrName = null;
				for (final String attributeName : attributeNames) {
					final String attribute = element.attr(attributeName);
					if (attribute != null && !"".equals(attribute)) {
						attr = attribute;
						attrName = attributeName;
						break;
					}
				}
				if (attr == null && "".equals(attr)) {
					break;
				}
				final String srcToDownload = element.absUrl(attrName);
				outerHtml = outerHtml.replace(attr, FileUtils.getNameFileFromURL(srcToDownload));
				sourcesToDownload.add(srcToDownload);
			}

			Pattern p = Pattern.compile("url\\((\\'|\\\")*.*(\\'|\\\")*\\)");
			Matcher m = p.matcher(outerHtml);
			while (m.find()) {
				String url = m.group();
				url = url.substring(0, url.indexOf(")", 0) + 1);
				final String clearUrl = url.replace("url(", "").replace("\"", "").replace("'", "").replace(")", "");
				final String srcToDownload = getUri(baseUri, clearUrl);
				outerHtml = outerHtml.replace(url, "url('" + FileUtils.getNameFileFromURL(srcToDownload) + "')");
				sourcesToDownload.add(srcToDownload);
			}

			storeStringOnFile(outerHtml, mediaFile.getAbsolutePath());
			if (!isNullOrEmpty(sourcesToDownload)) {
				for (final String sourceToDownload : sourcesToDownload) {
					final String fileName = FileUtils.getNameFileFromURL(sourceToDownload);
					final String filePath = new File(mediaFile.getParentFile(), fileName).getAbsolutePath();
					download(sourceToDownload, filePath);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getUri(final String baseUri, final String attr) {
		if (attr != null && attr.trim().startsWith("http")) {
			return attr;
		}

		if (baseUri != null && attr != null) {
			String uri = baseUri.substring(0, baseUri.lastIndexOf("/") + 1);
			final int matches = countMatches(attr, "../");
			for (int i = 0; i < matches; i++) {
				int lastIndex = uri.lastIndexOf("/");
				if (lastIndex == uri.length() - 1) {
					uri = uri.substring(0, lastIndex);
				}
				lastIndex = uri.lastIndexOf("/");
				uri = uri.substring(0, lastIndex);
			}
			final String endPoint = attr.replaceAll("^[\\.]{2}\\/", "");
			return String.valueOf(uri.charAt(uri.length() - 1)).equals("/") ? uri.concat(endPoint)
					: uri.concat("/").concat(endPoint);

		}

		return null;
	}

	public static boolean isNullOrEmpty(final Collection<?> c) {
		return c == null || c.isEmpty();
	}

	public static boolean isNullOrEmpty(final Object[] objects) {
		return objects == null || objects.length == 0;
	}

	public static int countMatches(final String source, final String find) {
		int lastIndex = 0;
		int count = 0;
		while (lastIndex != -1) {
			lastIndex = source.indexOf(find, lastIndex);
			if (lastIndex != -1) {
				count++;
				lastIndex += find.length();
			}
		}
		return count;
	}

	public static File download(final String urlFile, final String filePath) throws Exception {

		final URL url = new URL(urlFile);
		final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setConnectTimeout(5000);
		urlConnection.setRequestMethod("GET");
		urlConnection.setDoOutput(false);
		urlConnection.connect();

		final File file = new File(filePath.trim());
		try {
			if (file.exists() && file.isFile()) {
				file.delete();
			}
			file.getParentFile().mkdirs();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final InputStream is = urlConnection.getInputStream();
		final FileOutputStream fos = new FileOutputStream(file);

		long total = 0;
		final int lenghtOfFile = urlConnection.getContentLength();
		int bytesRead;
		byte[] buffer = new byte[50];
		while ((bytesRead = is.read(buffer)) != -1) {
			fos.write(buffer, 0, bytesRead);
		}

		fos.flush();
		fos.close();
		is.close();

		return file;
	}

	public static void deleteAllFilesFromDirectory(final File dir, final boolean selfDelete) {
		if (dir != null && dir.exists()) {
			if (dir.isFile() && selfDelete) {
				dir.delete();
			}

			if (dir.isDirectory()) {
				final File[] listFiles = dir.listFiles();
				if (listFiles != null && listFiles.length > 0) {
					for (final File file : listFiles) {
						deleteAllFilesFromDirectory(file, true);
					}
				}
				dir.delete();
			}
		}
	}

	public static void copyURLToFile(final String urlPage, final File downloadDir, final DonwloadTask donwloadTask,
			final boolean deleteDownloadDir) throws Exception {
		try {

			final File mediaFile = new File(downloadDir.getAbsolutePath(), "index.html");
			mkdirs(downloadDir.getAbsolutePath());

			if (deleteDownloadDir) {
				if (donwloadTask != null) {
					final List<String> processes = new ArrayList<>();
					processes.add("Limpando diretório antes do download " + urlPage);
					donwloadTask.process(processes);
				}
				deleteAllFilesFromDirectory(downloadDir, false);
			}

			if (donwloadTask != null) {
				final List<String> processes = new ArrayList<>();
				processes.add("Iniciando Download da página " + urlPage);
				donwloadTask.process(processes);
			}

			final Connection.Response response = Jsoup.connect(urlPage).execute();
			final Document doc = response.parse();
			final String baseUri = doc.baseUri();
			final ArrayList<String> sourcesToDownload = new ArrayList<>();
			final String[] attributeNames = new String[] { "src", "href" };

			if (donwloadTask != null) {
				final List<String> processes = new ArrayList<>();
				processes.add("\r\nListando arquivos a serem baixados ... ");
				donwloadTask.process(processes);
			}

			final Elements elements = new Elements();
			for (final String attributeName : attributeNames) {
				elements.addAll(doc.getElementsByAttribute(attributeName));
			}

			String outerHtml = doc.outerHtml();

			for (final Element element : elements) {
				String attr = null;
				String attrName = null;
				for (final String attributeName : attributeNames) {
					final String attribute = element.attr(attributeName);
					if (attribute != null && !"".equals(attribute)) {
						attr = attribute;
						attrName = attributeName;
						break;
					}
				}
				if (attr == null && "".equals(attr)) {
					break;
				}
				final String srcToDownload = element.absUrl(attrName);
				outerHtml = outerHtml.replace(attr, FileUtils.getNameFileFromURL(srcToDownload));
				sourcesToDownload.add(srcToDownload);

				if (donwloadTask != null) {
					final List<String> processes = new ArrayList<>();
					processes.add(srcToDownload);
					donwloadTask.process(processes);
				}

			}

			Pattern p = Pattern.compile("url\\((\\'|\\\")*.*(\\'|\\\")*\\)");
			Matcher m = p.matcher(outerHtml);
			while (m.find()) {
				String url = m.group();
				url = url.substring(0, url.indexOf(")", 0) + 1);
				final String clearUrl = url.replace("url(", "").replace("\"", "").replace("'", "").replace(")", "");
				final String srcToDownload = getUri(baseUri, clearUrl);
				outerHtml = outerHtml.replace(url, "url('" + FileUtils.getNameFileFromURL(srcToDownload) + "')");
				sourcesToDownload.add(srcToDownload);

				if (donwloadTask != null) {
					final List<String> processes = new ArrayList<>();
					processes.add(srcToDownload);
					donwloadTask.process(processes);
				}
			}

			if (!isNullOrEmpty(sourcesToDownload)) {
				if (donwloadTask != null) {
					final List<String> processes = new ArrayList<>();
					processes.add("\r\nBaixando arquivos (" + sourcesToDownload.size() + " no total)");
					donwloadTask.process(processes);
				}

				for (final String sourceToDownload : sourcesToDownload) {
					final String fileName = FileUtils.getNameFileFromURL(sourceToDownload);
					final String filePath = new File(downloadDir, fileName).getAbsolutePath();

					if (donwloadTask != null) {
						final List<String> processes = new ArrayList<>();
						processes.add("Baixando arquivo " + sourceToDownload + " para " + filePath);
						donwloadTask.process(processes);
					}
					download(sourceToDownload, filePath);
				}
			}

			if (donwloadTask != null) {
				final List<String> processes = new ArrayList<>();
				processes.add("Gravando index.html");
				donwloadTask.process(processes);
			}
			storeStringOnFile(outerHtml, mediaFile.getAbsolutePath());

			if (donwloadTask != null) {
				final List<String> processes = new ArrayList<>();
				processes.add("\r\nDownload concluído.\r\n");
				donwloadTask.process(processes);
			}

		} catch (Exception e) {
			e.printStackTrace();
			if (donwloadTask != null) {
				final List<String> processes = new ArrayList<>();
				processes.add("\r\nErro de Download " + e.getMessage() + "\r\n");
				donwloadTask.process(processes);
			}
			throw e;
		}
	}

}
