package com.mycompany.mapstruct;

import org.mapstruct.Mapper;

import com.mycompany.config.MapStructConfig;
import com.mycompany.dto.response.GameProfileResponse;
import com.mycompany.dto.response.LeaderboardItemResponse;
import com.mycompany.entity.UserEntity;
import com.mycompany.repository.UserRepository;

@Mapper(config = MapStructConfig.class)
public interface GameMapper {

    GameProfileResponse toGameProfileResponse(UserEntity user);

    LeaderboardItemResponse toLeaderboardItemResponse(UserRepository.LeaderboardProjection projection);
}
