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
        version = "0.28.0";
        assets = {
          aarch64-darwin = {
            file = "solidcraft-cli-0.28.0-macos-universal.zip";
            sha256 = "c10f6cb9dd41fbbba1237cce6c09594346114e30c71c8c6d9d531823c86ff0ba";
          };
          x86_64-darwin = {
            file = "solidcraft-cli-0.28.0-macos-universal.zip";
            sha256 = "c10f6cb9dd41fbbba1237cce6c09594346114e30c71c8c6d9d531823c86ff0ba";
          };
          aarch64-linux = {
            file = "solidcraft-0.28.0-linux-aarch64.tar.gz";
            sha256 = "8bfaf9cef605cf79a61a27220ddc22ebd36b9f6174a731880f97d1e97255ed9f";
          };
          x86_64-linux = {
            file = "solidcraft-0.28.0-linux-x86_64.tar.gz";
            sha256 = "46bb3ec650c21c3377cb4df264b6426c4a62cad8b68a0fe0c66fd16345257678";
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
