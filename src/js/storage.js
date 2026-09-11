export const StorageService = {
    getCurrentUserId() {
        return localStorage.getItem("currentUserId") || "demo-user";
    },
    getRecipes(userId) {
        return JSON.parse(localStorage.getItem(`recipes_${userId}`)) || [];
    },
    saveRecipes(userId, recipes) {
        localStorage.setItem(`recipes_${userId}`, JSON.stringify(recipes));
    },
    getCookbooks(userId) {
        return JSON.parse(localStorage.getItem(`cookbooks_${userId}`)) || [];
    },
    saveCookbooks(userId, cookbooks) {
        localStorage.setItem(`cookbooks_${userId}`, JSON.stringify(cookbooks));
    },
    getPantry(userId) {
        return JSON.parse(localStorage.getItem(`pantry_${userId}`)) || [];
    },
    savePantry(userId, pantry) {
        localStorage.setItem(`pantry_${userId}`, JSON.stringify(pantry));
    }
};
