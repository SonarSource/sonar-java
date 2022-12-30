load("github.com/SonarSource/cirrus-modules@v2", "load_features")

def main(ctx):
  return load_features(ctx, features=["aws", "vault"], aws=dict(env_type="dev", cluster_name="CirrusCI-1-pr-29", subnet_id="subnet-09ae6243cee1cf5d6"))
