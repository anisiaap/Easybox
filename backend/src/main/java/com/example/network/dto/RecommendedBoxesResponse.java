package com.example.network.dto;

import java.util.List;

public class RecommendedBoxesResponse {
    private EasyboxDto recommendedBox;      // the top match
    private List<EasyboxDto> otherBoxes;    // all other matching boxes

    public RecommendedBoxesResponse() {}

    public RecommendedBoxesResponse(EasyboxDto recommendedBox, List<EasyboxDto> otherBoxes) {
        this.recommendedBox = recommendedBox;
        this.otherBoxes = otherBoxes;
    }

    public EasyboxDto getRecommendedBox() { return recommendedBox; }
    public void setRecommendedBox(EasyboxDto recommendedBox) { this.recommendedBox = recommendedBox; }

    public List<EasyboxDto> getOtherBoxes() { return otherBoxes; }
    public void setOtherBoxes(List<EasyboxDto> otherBoxes) { this.otherBoxes = otherBoxes; }
}
