{
  description = "solidcraft-dsl: a declarative text format for SolidCraft designs, with a compiler, decompiler, checker and live sync";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      eachSystem = nixpkgs.lib.genAttrs [
        "aarch64-darwin"
        "x86_64-darwin"
        "aarch64-linux"
        "x86_64-linux"
      ];

      # The SolidCraft release this crate is developed and tested against. The compiler
      # predicts engine behaviour (entity id allocation, dimension numbering, command
      # parameters) that was measured on this version; bump it here, run the tests, and update
      # the version in README.md. Hashes are from the release's SHA256SUMS.txt.
      solidcraft = {
        version = "0.27.1";
        assets = {
          aarch64-darwin = {
            file = "solidcraft-cli-0.27.1-macos-universal.zip";
            sha256 = "98222a069979388bf9ec7f432d214f0ad1911efff28cad7b439872f8c658b3b6";
          };
          x86_64-darwin = {
            file = "solidcraft-cli-0.27.1-macos-universal.zip";
            sha256 = "98222a069979388bf9ec7f432d214f0ad1911efff28cad7b439872f8c658b3b6";
          };
          aarch64-linux = {
            file = "solidcraft-0.27.1-linux-aarch64.tar.gz";
            sha256 = "8813aa03d67c2cd7486a206dca73bb1c888e38dfd78ad8b14e58ee0285ab90a3";
          };
          x86_64-linux = {
            file = "solidcraft-0.27.1-linux-x86_64.tar.gz";
            sha256 = "bd4358dedee6d662dc7565eb84f978810b8940612bf156100b97cb46d01368ea";
          };
        };
      };

      # `solidcraft-cli` from the pinned release: the headless engine the tests run against.
      solidcraftCli =
        pkgs: system:
        let
          asset = solidcraft.assets.${system};
          src = pkgs.fetchurl {
            url = "https://github.com/tangency-labs/solidcraft/releases/download/v${solidcraft.version}/${asset.file}";
            inherit (asset) sha256;
          };
          isDarwin = pkgs.stdenv.isDarwin;
        in
        pkgs.stdenv.mkDerivation {
          pname = "solidcraft-cli";
          inherit (solidcraft) version;
          inherit src;
          nativeBuildInputs =
            (if isDarwin then [ pkgs.unzip ] else [ pkgs.autoPatchelfHook ]);
          buildInputs = if isDarwin then [ ] else [ pkgs.stdenv.cc.cc.lib ];
          sourceRoot = ".";
          unpackPhase =
            if isDarwin then
              "unzip -q $src"
            else
              "tar -xzf $src --strip-components=1";
          installPhase = ''
            mkdir -p $out/bin
            cli=$(find . -type f -name solidcraft-cli | head -1)
            [ -n "$cli" ] || { echo "solidcraft-cli not found in ${asset.file}"; exit 1; }
            install -m 755 "$cli" $out/bin/solidcraft-cli
          '';
          meta = {
            description = "SolidCraft headless CLI (pinned release ${solidcraft.version})";
            homepage = "https://github.com/tangency-labs/solidcraft";
            platforms = [ system ];
          };
        };
    in
    {
      packages = eachSystem (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
        in
        {
          solidcraft-cli = solidcraftCli pkgs system;
          default = pkgs.rustPlatform.buildRustPackage {
            pname = "solidcraft-dsl";
            version = "0.1.0";
            src = ./.;
            cargoLock.lockFile = ./Cargo.lock;
            # The engine tests need solidcraft-cli; they skip without it, and `nix flake check`
            # runs them with the pinned one.
            nativeCheckInputs = [ (solidcraftCli pkgs system) ];
            SOLIDCRAFT_CLI = "${solidcraftCli pkgs system}/bin/solidcraft-cli";
          };
        }
      );

      devShells = eachSystem (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          cli = solidcraftCli pkgs system;
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              cargo
              rustc
              rustfmt
              clippy
              rust-analyzer
              cli
            ];
            RUST_SRC_PATH = "${pkgs.rustPlatform.rustLibSrc}";
            SOLIDCRAFT_CLI = "${cli}/bin/solidcraft-cli";
            SOLIDCRAFT_VERSION = solidcraft.version;
          };
        }
      );

      formatter = eachSystem (system: nixpkgs.legacyPackages.${system}.nixfmt);
    };
}
