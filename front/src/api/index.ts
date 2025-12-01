import api from "./axios";

export const submitAnalysisAPI = (image: File, survey: string[]) => {
  const form = new FormData();
  form.append("image_url", image);
  form.append("survey", JSON.stringify(survey));
  return api.post("/analysis/image", form, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};
