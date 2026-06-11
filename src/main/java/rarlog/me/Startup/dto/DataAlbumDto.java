package rarlog.me.Startup.dto;

import java.util.List;

import lombok.Data;

@Data
public class DataAlbumDto {

    private String name;
    private String coverPath;
    private List<DataSongDto> songs;

}
