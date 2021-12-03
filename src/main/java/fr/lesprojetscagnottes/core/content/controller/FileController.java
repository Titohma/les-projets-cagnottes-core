package fr.lesprojetscagnottes.core.content.controller;

import fr.lesprojetscagnottes.core.common.exception.BadRequestException;
import fr.lesprojetscagnottes.core.common.strings.StringGenerator;
import fr.lesprojetscagnottes.core.content.entity.FileEntity;
import fr.lesprojetscagnottes.core.content.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Tag(name = "Files", description = "The Files API")
@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/files/**", method = RequestMethod.GET, produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public @ResponseBody byte[] getImage(HttpServletRequest request) throws IOException {
        return fileService.readOnFilesystem(request);
    }

    @RequestMapping(value = "/api/files/image", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public String uploadImage(
            @RequestParam("directory") String directory,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam("image") MultipartFile multipartFile) {


        if (directory == null || multipartFile == null) {
            throw new BadRequestException();
        }

        if(name == null) {
            name = StringGenerator.imageName();
        }

        FileEntity entity = new FileEntity();
        try {
            entity = fileService.saveOnFilesystem(multipartFile, directory, name);
            entity = fileService.saveInDb(entity);
        } catch (IOException e) {
            log.error("Cannot save file {}", multipartFile.getName(), e);
        }

        return entity.getUrl();
    }

    @RequestMapping(value = "/api/files/{id}", method = RequestMethod.DELETE)
    public Boolean delete(@PathVariable("id") Long id) {
        return fileService.delete(id);
    }


}
